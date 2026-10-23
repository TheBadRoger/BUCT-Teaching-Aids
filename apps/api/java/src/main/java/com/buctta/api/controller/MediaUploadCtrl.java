package com.buctta.api.controller;

import com.buctta.api.dto.MediaFileDTO;
import com.buctta.api.service.MediaPurpose;
import com.buctta.api.service.MediaUploadService;
import com.buctta.api.service.MediaValidationException;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传接口。
 * <p>
 * 用于把原先「手填 URL」的字段改为直接上传文件：
 * 上传成功后服务端会把访问地址回填到对应业务字段
 * （如 {@code course_list.course_image}、{@code organization_list.banner_url}），
 * 前端只需刷新业务对象即可拿到新地址。
 *
 * @see MediaPurpose 各用途对应的校验规则与回填字段
 */
@Slf4j
@RestController
@RequestMapping("/api/media")
@CrossOrigin
public class MediaUploadCtrl {

    @Resource
    private MediaUploadService mediaUploadService;

    /**
     * 通用上传。
     *
     * @param file    文件，表单字段名固定为 {@code file}
     * @param purpose 用途，见 {@link MediaPurpose}：
     *                {@code VIDEO} / {@code COURSE_COVER} / {@code ORG_LOGO} /
     *                {@code ORG_BANNER} / {@code ORG_HONOR_CERT}
     * @param ownerId 业务主体 ID：{@code COURSE_COVER} 传课程 ID，
     *                {@code ORG_*} 传机构 ID，{@code VIDEO} 可省略
     */
    @PostMapping("/upload")
    public ApiResponse<MediaUploadService.UploadResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("purpose") String purpose,
            @RequestParam(value = "ownerId", required = false) String ownerId) {
        return doUpload(file, purpose, ownerId);
    }

    /**
     * 图片上传便捷入口，等价于 {@code purpose} 为图片类用途的 {@code /upload}。
     * 显式声明 {@code MultipartFile} 参数以便正确解析 multipart 请求。
     */
    @PostMapping(path = "/upload/image", consumes = "multipart/form-data")
    public ApiResponse<MediaUploadService.UploadResult> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("purpose") String purpose,
            @RequestParam(value = "ownerId", required = false) String ownerId) {
        return doUpload(file, purpose, ownerId);
    }

    private ApiResponse<MediaUploadService.UploadResult> doUpload(MultipartFile file,
                                                                  String purpose,
                                                                  String ownerId) {
        MediaPurpose parsed = MediaPurpose.parse(purpose);
        if (parsed == null) {
            return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR,
                    "无法识别的 purpose: " + purpose + "，可选值: VIDEO, COURSE_COVER, "
                            + "ORG_LOGO, ORG_BANNER, ORG_HONOR_CERT");
        }

        Long parsedOwnerId = parseId(ownerId);
        if (ownerId != null && !ownerId.isBlank() && parsedOwnerId == null) {
            return ApiResponse.fail(BusinessStatus.PARAM_TYPE_ERROR, "ownerId 必须是数字");
        }

        try {
            return ApiResponse.ok(mediaUploadService.upload(file, parsed, parsedOwnerId));
        }
        catch (MediaValidationException e) {
            return ApiResponse.fail(e.status(), e.getMessage());
        }
        catch (IOException e) {
            log.error("上传文件落盘失败 purpose={}", purpose, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR, "文件保存失败: " + e.getMessage());
        }
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
