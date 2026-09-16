package com.buctta.api.controller;

import com.buctta.api.dto.MediaFileDTO;
import com.buctta.api.entities.MediaFile;
import com.buctta.api.service.MediaStorageService;
import com.buctta.api.service.MediaUploadService;
import com.buctta.api.service.MediaValidationException;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.ByteRange;
import com.buctta.api.utils.BusinessStatus;
import com.buctta.api.utils.FileTypeSniffer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 媒体读取：图片直出、视频支持 HTTP Range 分片（拖动进度条、断点续播）。
 * <p>
 * 说明：当前实现按请求区间读取到内存后返回。教学视频单文件通常几百 MB 以内可用；
 * 若要支持超大文件，可把 {@code writeTo} 换成
 * {@code ResourceRegion} + {@code ResourceHttpRequestHandler} 实现零拷贝分片。
 */
@Slf4j
@RestController
@RequestMapping("/api/media")
public class MediaStreamCtrl {

    @Resource
    private MediaUploadService mediaUploadService;

    @Resource
    private MediaStorageService mediaStorageService;

    /** 浏览器缓存时长：媒体文件不可变，按 id 访问，长缓存安全 */
    private static final long CACHE_SECONDS = 7L * 24 * 60 * 60;

    /**
     * 读取媒体内容。
     * 带 {@code Range} 头时返回 {@code 206 Partial Content}，
     * 否则返回 {@code 200} 全量。区间不可满足返回 {@code 416}。
     * <p>
     * 这里用具体类型 {@link ByteArrayResource} 作为泛型参数，而不是 Spring 的
     * {@code Resource} 接口 —— 后者与字段注入用的 {@code jakarta.annotation.Resource}
     * 同名，同时导入会产生歧义。
     */
    @GetMapping("/{id}/content")
    public ResponseEntity<ByteArrayResource> content(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader)
            throws IOException {
        MediaFile media = mediaUploadService.getMediaFile(id);
        if (!mediaStorageService.exists(media.getStorageKey())) {
            return ResponseEntity.notFound().build();
        }

        long fileSize = media.getFileSize();
        HttpHeaders headers = baseHeaders(media, fileSize);

        ByteRange range = ByteRange.parse(rangeHeader, fileSize);
        if (rangeHeader != null && range == null) {
            // 有 Range 请求但不可满足（越界/格式错误），按 RFC 返回 416
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
            return new ResponseEntity<>(null, headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        if (range == null) {
            byte[] body = read(media.getStorageKey(), 0, fileSize);
            headers.setContentLength(body.length);
            return new ResponseEntity<>(new ByteArrayResource(body), headers, HttpStatus.OK);
        }

        byte[] body = read(media.getStorageKey(), range.start(), range.length());
        headers.set(HttpHeaders.CONTENT_RANGE, range.contentRange(fileSize));
        headers.setContentLength(body.length);
        return new ResponseEntity<>(new ByteArrayResource(body), headers, HttpStatus.PARTIAL_CONTENT);
    }

    /**
     * 仅返回响应头，供播放器探测文件大小与是否支持 Range。
     */
    @RequestMapping(value = "/{id}/content", method = RequestMethod.HEAD)
    public ResponseEntity<Void> head(@PathVariable Long id) {
        MediaFile media = mediaUploadService.getMediaFile(id);
        if (!mediaStorageService.exists(media.getStorageKey())) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = baseHeaders(media, media.getFileSize());
        headers.setContentLength(media.getFileSize());
        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    /** 文件元数据 */
    @GetMapping("/{id}")
    public ApiResponse<MediaFileDTO> info(@PathVariable Long id) {
        try {
            MediaFile media = mediaUploadService.getMediaFile(id);
            String url = "/api/media/" + media.getId() + "/content";
            return ApiResponse.ok(MediaFileDTO.of(media, url));
        }
        catch (MediaValidationException e) {
            return ApiResponse.fail(e.status(), e.getMessage());
        }
    }

    /**
     * 删除媒体文件。
     * <p>
     * 注意：本接口只删除文件本体与元数据，不会清空业务字段中原先写入的地址，
     * 因此课程封面/机构图片请优先通过重新上传覆盖，或在删除后同步更新业务字段。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        try {
            mediaUploadService.delete(id);
            return ApiResponse.ok("媒体文件已删除，ID: " + id);
        }
        catch (MediaValidationException e) {
            return ApiResponse.fail(e.status(), e.getMessage());
        }
        catch (RuntimeException e) {
            log.error("删除媒体文件失败 id={}", id, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /* ------------------------------------------------------------ */

    private HttpHeaders baseHeaders(MediaFile media, long fileSize) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(safeContentType(media)));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_SECONDS);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename*=UTF-8''" + encodeFilename(media.getOriginalFilename()));
        // 防止浏览器把媒体内容按扩展名猜测成可执行类型
        headers.set("X-Content-Type-Options", "nosniff");
        return headers;
    }

    private String safeContentType(MediaFile media) {
        String contentType = media.getContentType();
        if (contentType == null || contentType.isBlank()
                || FileTypeSniffer.TYPE_UNKNOWN.equals(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        try {
            MediaType.parseMediaType(contentType);
            return contentType;
        }
        catch (RuntimeException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    private String encodeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private byte[] read(String storageKey, long offset, long length) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(
                (int) Math.min(length, 8L * 1024 * 1024));
        mediaStorageService.writeTo(storageKey, offset, length, buffer);
        return buffer.toByteArray();
    }
}
