package com.buctta.api.serviceimp;

import com.buctta.api.config.MediaProperties;
import com.buctta.api.dao.CourseReposit;
import com.buctta.api.dao.MediaFileRepository;
import com.buctta.api.dao.OrganizationRepository;
import com.buctta.api.dto.MediaFileDTO;
import com.buctta.api.entities.Course;
import com.buctta.api.entities.MediaFile;
import com.buctta.api.entities.Organization;
import com.buctta.api.service.MediaPurpose;
import com.buctta.api.service.MediaStorageService;
import com.buctta.api.service.MediaUploadService;
import com.buctta.api.service.MediaValidationException;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 上传落库 + 业务字段回填。
 * <p>
 * 回填策略：文件先落盘、再入库，最后把访问地址写进业务字段。任一环节失败都会
 * 清理已落盘的文件，避免留下无主文件。
 */
@Slf4j
@Service
public class IMPL_MediaUploadService implements MediaUploadService {

    /** 相对路径形式，同源前端可直接使用，也便于反向代理与域名变更 */
    private static final String CONTENT_PATH = "/api/media/%d/content";

    @Resource
    private MediaStorageService mediaStorageService;

    @Resource
    private MediaFileRepository mediaFileRepository;

    @Resource
    private CourseReposit courseReposit;

    @Resource
    private OrganizationRepository organizationRepository;

    @Resource
    private MediaProperties mediaProperties;

    @Override
    @Transactional
    public UploadResult upload(MultipartFile file, MediaPurpose purpose, Long ownerId)
            throws IOException {
        if (purpose == null) {
            throw new MediaValidationException(BusinessStatus.PARAM_MISSING, "purpose 不能为空");
        }

        MediaStorageService.StoredFile stored = mediaStorageService.store(file, purpose);
        MediaFile saved = null;

        try {
            saved = mediaFileRepository.save(mediaStorageService.toEntity(stored));
            String url = mediaUrl(saved.getId());

            String targetField = null;
            if (purpose != MediaPurpose.VIDEO) {
                targetField = applyToOwner(purpose, ownerId, url);
            }

            MediaFileDTO dto = new MediaFileDTO(saved.getId(),
                    saved.getOriginalFilename(),
                    url,
                    saved.getContentType(),
                    saved.getFileSize(),
                    saved.getKind(),
                    purpose.name(),
                    targetField,
                    targetField == null ? null : url);

            return new UploadResult(dto, purpose.name(), targetField);
        }
        catch (MediaValidationException e) {
            // 回填失败：连同数据库记录与磁盘文件一起回滚，避免留下无主的 media_file
            rollback(saved, stored.storageKey());
            throw e;
        }
        catch (RuntimeException e) {
            rollback(saved, stored.storageKey());
            log.error("媒体入库失败 purpose={} ownerId={}", purpose, ownerId, e);
            throw new MediaValidationException(BusinessStatus.DATABASE_ERROR,
                    "媒体入库失败: " + e.getMessage());
        }
    }

    /**
     * 回滚一次失败的上传。
     * <p>
     * 必须"先删记录、再删文件"：只删文件会留下指向不存在文件的脏记录，
     * 后续按 id 读取时表现为 404。
     */
    private void rollback(MediaFile saved, String storageKey) {
        if (saved != null && saved.getId() != null) {
            try {
                mediaFileRepository.delete(saved);
            }
            catch (RuntimeException e) {
                log.warn("回滚媒体记录失败 id={}", saved.getId(), e);
            }
        }
        mediaStorageService.deleteQuietly(List.of(storageKey));
    }

    @Override
    public MediaFile getMediaFile(Long id) {
        return mediaFileRepository.findById(id)
                .orElseThrow(() -> new MediaValidationException(BusinessStatus.RESOURCE_NOT_FOUND,
                        "媒体文件不存在，ID: " + id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MediaFile media = getMediaFile(id);
        mediaFileRepository.delete(media);
        mediaStorageService.delete(media.getStorageKey());
    }

    /** 把上传结果写进对应的业务字段，返回被写入的字段名 */
    private String applyToOwner(MediaPurpose purpose, Long ownerId, String url) {
        if (ownerId == null) {
            throw new MediaValidationException(BusinessStatus.PARAM_MISSING,
                    "用途 " + purpose.name() + " 需要提供 ownerId");
        }

        switch (purpose) {
            case COURSE_COVER -> {
                Course course = courseReposit.findById(ownerId)
                        .orElseThrow(() -> new MediaValidationException(
                                BusinessStatus.RESOURCE_NOT_FOUND, "课程不存在，ID: " + ownerId));
                course.setCourseImage(url);
                courseReposit.save(course);
                return purpose.fieldName();
            }
            case ORG_LOGO, ORG_BANNER, ORG_HONOR_CERT -> {
                Organization org = organizationRepository.findById(ownerId)
                        .orElseThrow(() -> new MediaValidationException(
                                BusinessStatus.RESOURCE_NOT_FOUND, "机构不存在，ID: " + ownerId));
                switch (purpose) {
                    case ORG_LOGO -> org.setLogo(url);
                    case ORG_BANNER -> org.setBannerUrl(url);
                    case ORG_HONOR_CERT -> org.setHonorCertUrl(url);
                    default -> throw new MediaValidationException(
                            BusinessStatus.PARAM_FORMAT_ERROR, "不支持的用途: " + purpose);
                }
                organizationRepository.save(org);
                return purpose.fieldName();
            }
            case VIDEO -> {
                // 视频由 CourseVideoService 负责建目录条目，此处不回填
                return null;
            }
            default -> throw new MediaValidationException(BusinessStatus.PARAM_FORMAT_ERROR,
                    "不支持的用途: " + purpose);
        }
    }

    /** 生成对外访问地址 */
    private String mediaUrl(Long mediaId) {
        String prefix = mediaProperties.getPublicUrlPrefix();
        if (prefix == null || prefix.isBlank()) {
            return String.format(CONTENT_PATH, mediaId);
        }
        String normalized = prefix.endsWith("/")
                ? prefix.substring(0, prefix.length() - 1)
                : prefix;
        return normalized + String.format(CONTENT_PATH, mediaId);
    }
}
