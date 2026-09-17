package com.buctta.api.dto;

import com.buctta.api.entities.MediaFile;
import com.buctta.api.entities.MediaKind;

/**
 * 上传成功后的返回体，前端拿到 {@link #url} 即可直接放进
 * {@code <img src>}、CSS 背景或 {@code <video src>}。
 */
public record MediaFileDTO(
        Long fileId,
        String fileName,
        String url,
        String contentType,
        Long size,
        MediaKind kind,
        /** 若上传时带了 purpose，表示该文件是干什么用的；否则为 null */
        String purpose,
        /** 该文件应当回填的业务字段名，便于前端对照；无则为 null */
        String targetField,
        /** 该文件对应实体上字段的最终取值（通常等于 url） */
        String storedValue
) {

    public static MediaFileDTO of(MediaFile media, String url) {
        return new MediaFileDTO(media.getId(),
                media.getOriginalFilename(),
                url,
                media.getContentType(),
                media.getFileSize(),
                media.getKind(),
                null,
                null,
                null);
    }
}
