package com.buctta.api.serviceimp;

import com.buctta.api.dao.CourseVideoRepository;
import com.buctta.api.dao.CourseReposit;
import com.buctta.api.dao.MediaFileRepository;
import com.buctta.api.dto.MediaFileDTO;
import com.buctta.api.entities.CourseVideo;
import com.buctta.api.entities.MediaFile;
import com.buctta.api.service.CourseVideoService;
import com.buctta.api.service.MediaPurpose;
import com.buctta.api.service.MediaStorageService;
import com.buctta.api.service.MediaValidationException;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程多视频实现。
 */
@Slf4j
@Service
public class IMPL_CourseVideoService implements CourseVideoService {

    private static final String CONTENT_PATH = "/api/media/%d/content";

    @Resource
    private MediaStorageService mediaStorageService;

    @Resource
    private MediaFileRepository mediaFileRepository;

    @Resource
    private CourseVideoRepository courseVideoRepository;

    @Resource
    private CourseReposit courseReposit;

    @Override
    @Transactional
    public CourseVideoDetail uploadVideo(Long courseId, String title, String description,
                                         Integer sortOrder, Integer durationSeconds,
                                         MultipartFile video, MultipartFile cover,
                                         boolean withCover) throws IOException {
        if (courseId == null) {
            throw new MediaValidationException(BusinessStatus.PARAM_MISSING, "courseId 不能为空");
        }
        if (!courseReposit.existsById(courseId)) {
            throw new MediaValidationException(BusinessStatus.RESOURCE_NOT_FOUND,
                    "课程不存在，ID: " + courseId);
        }
        if (video == null || video.isEmpty()) {
            throw new MediaValidationException(BusinessStatus.PARAM_MISSING, "视频文件不能为空");
        }

        // 回滚需要同时回收"数据库记录 + 磁盘文件"，因此这里保存 media_file 实体
        List<MediaFile> rollback = new ArrayList<>(2);
        try {
            MediaStorageService.StoredFile storedVideo =
                    mediaStorageService.store(video, MediaPurpose.VIDEO);
            MediaFile savedVideo = mediaFileRepository.save(mediaStorageService.toEntity(storedVideo));
            rollback.add(savedVideo);

            MediaFile savedCover = null;
            if (withCover) {
                if (cover == null || cover.isEmpty()) {
                    throw new MediaValidationException(BusinessStatus.PARAM_MISSING, "封面文件为空");
                }
                MediaStorageService.StoredFile storedCover =
                        mediaStorageService.store(cover, MediaPurpose.COURSE_COVER);
                savedCover = mediaFileRepository.save(mediaStorageService.toEntity(storedCover));
                rollback.add(savedCover);
            }

            CourseVideo entity = new CourseVideo();
            entity.setCourseId(courseId);
            entity.setTitle(StringUtils.hasText(title) ? title.trim()
                    : defaultTitle(savedVideo.getOriginalFilename()));
            entity.setDescription(description);
            entity.setSortOrder(sortOrder != null ? sortOrder : nextSortOrder(courseId));
            entity.setDurationSeconds(durationSeconds);
            entity.setVideoFileId(savedVideo.getId());
            entity.setCoverFileId(savedCover == null ? null : savedCover.getId());

            CourseVideo saved = courseVideoRepository.save(entity);
            return toDetail(saved, savedVideo, savedCover);
        }
        catch (MediaValidationException e) {
            rollbackMedia(rollback);
            throw e;
        }
        catch (RuntimeException e) {
            rollbackMedia(rollback);
            log.error("课程视频入库失败 courseId={}", courseId, e);
            throw new MediaValidationException(BusinessStatus.DATABASE_ERROR,
                    "课程视频入库失败: " + e.getMessage());
        }
    }

    /**
     * 回滚一次失败的入库。
     * <p>
     * 必须"先删记录、再删文件"：只删文件会留下指向不存在文件的脏 media_file 记录。
     */
    private void rollbackMedia(List<MediaFile> mediaFiles) {
        for (MediaFile media : mediaFiles) {
            if (media == null || media.getId() == null) {
                continue;
            }
            try {
                mediaFileRepository.delete(media);
            }
            catch (RuntimeException e) {
                log.warn("回滚媒体记录失败 id={}", media.getId(), e);
            }
            mediaStorageService.delete(media.getStorageKey());
        }
    }

    @Override
    public List<CourseVideoDetail> listByCourse(Long courseId) {
        if (courseId == null) {
            throw new MediaValidationException(BusinessStatus.PARAM_MISSING, "courseId 不能为空");
        }
        if (!courseReposit.existsById(courseId)) {
            throw new MediaValidationException(BusinessStatus.RESOURCE_NOT_FOUND,
                    "课程不存在，ID: " + courseId);
        }

        List<CourseVideo> videos = courseVideoRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId);
        if (videos.isEmpty()) {
            return List.of();
        }

        List<Long> fileIds = new ArrayList<>(videos.size() * 2);
        for (CourseVideo video : videos) {
            fileIds.add(video.getVideoFileId());
            if (video.getCoverFileId() != null) {
                fileIds.add(video.getCoverFileId());
            }
        }
        var fileMap = new java.util.HashMap<Long, MediaFile>();
        mediaFileRepository.findByIdIn(fileIds).forEach(f -> fileMap.put(f.getId(), f));

        List<CourseVideoDetail> result = new ArrayList<>(videos.size());
        for (CourseVideo video : videos) {
            result.add(toDetail(video,
                    fileMap.get(video.getVideoFileId()),
                    video.getCoverFileId() == null ? null : fileMap.get(video.getCoverFileId())));
        }
        return result;
    }

    @Override
    public CourseVideoDetail getDetail(Long videoId) {
        CourseVideo video = requireVideo(videoId);
        MediaFile videoFile = mediaFileRepository.findById(video.getVideoFileId()).orElse(null);
        MediaFile coverFile = video.getCoverFileId() == null
                ? null
                : mediaFileRepository.findById(video.getCoverFileId()).orElse(null);
        return toDetail(video, videoFile, coverFile);
    }

    @Override
    @Transactional
    public CourseVideoDetail updateVideo(Long videoId, String title, String description,
                                         Integer sortOrder, Integer durationSeconds,
                                         MultipartFile cover, boolean replaceCover) throws IOException {
        CourseVideo video = requireVideo(videoId);
        MediaFile videoFile = mediaFileRepository.findById(video.getVideoFileId()).orElse(null);
        MediaFile coverFile = video.getCoverFileId() == null
                ? null
                : mediaFileRepository.findById(video.getCoverFileId()).orElse(null);

        if (StringUtils.hasText(title)) {
            video.setTitle(title.trim());
        }
        if (description != null) {
            video.setDescription(description);
        }
        if (sortOrder != null) {
            video.setSortOrder(sortOrder);
        }
        if (durationSeconds != null) {
            video.setDurationSeconds(durationSeconds);
        }

        List<String> rollback = new ArrayList<>(1);
        Long previousCoverId = video.getCoverFileId();

        if (replaceCover) {
            if (cover == null || cover.isEmpty()) {
                throw new MediaValidationException(BusinessStatus.PARAM_MISSING, "封面文件为空");
            }
            MediaStorageService.StoredFile storedCover =
                    mediaStorageService.store(cover, MediaPurpose.COURSE_COVER);
            rollback.add(storedCover.storageKey());
            MediaFile savedCover = mediaFileRepository.save(mediaStorageService.toEntity(storedCover));
            video.setCoverFileId(savedCover.getId());
            coverFile = savedCover;
        }

        try {
            CourseVideo saved = courseVideoRepository.save(video);

            // 换封面成功后清理旧封面，避免产生无主文件
            if (replaceCover && previousCoverId != null && !previousCoverId.equals(saved.getCoverFileId())) {
                mediaFileRepository.findById(previousCoverId).ifPresent(old -> {
                    mediaFileRepository.delete(old);
                    mediaStorageService.delete(old.getStorageKey());
                });
            }
            return toDetail(saved, videoFile, coverFile);
        }
        catch (RuntimeException e) {
            mediaStorageService.deleteQuietly(rollback);
            throw new MediaValidationException(BusinessStatus.DATABASE_ERROR,
                    "课程视频更新失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteVideo(Long videoId) {
        CourseVideo video = requireVideo(videoId);
        Long videoFileId = video.getVideoFileId();
        Long coverFileId = video.getCoverFileId();

        courseVideoRepository.delete(video);
        cleanupFile(videoFileId);
        if (coverFileId != null) {
            cleanupFile(coverFileId);
        }
    }

    /* ------------------------------------------------------------ */

    private CourseVideo requireVideo(Long videoId) {
        if (videoId == null) {
            throw new MediaValidationException(BusinessStatus.PARAM_MISSING, "videoId 不能为空");
        }
        return courseVideoRepository.findById(videoId)
                .orElseThrow(() -> new MediaValidationException(BusinessStatus.RESOURCE_NOT_FOUND,
                        "课程视频不存在，ID: " + videoId));
    }

    private void cleanupFile(Long fileId) {
        mediaFileRepository.findById(fileId).ifPresent(media -> {
            mediaFileRepository.delete(media);
            mediaStorageService.delete(media.getStorageKey());
        });
    }

    private int nextSortOrder(Long courseId) {
        return (int) courseVideoRepository.countByCourseId(courseId);
    }

    private String defaultTitle(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "未命名视频";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot > 0 ? originalFilename.substring(0, dot) : originalFilename;
    }

    private CourseVideoDetail toDetail(CourseVideo video, MediaFile videoFile, MediaFile coverFile) {
        return CourseVideoDetail.of(video,
                videoFile == null ? null : String.format(CONTENT_PATH, videoFile.getId()),
                coverFile == null ? null : String.format(CONTENT_PATH, coverFile.getId()),
                videoFile == null ? null : MediaFileDTO.of(videoFile, String.format(CONTENT_PATH, videoFile.getId())),
                coverFile == null ? null : MediaFileDTO.of(coverFile, String.format(CONTENT_PATH, coverFile.getId())));
    }
}
