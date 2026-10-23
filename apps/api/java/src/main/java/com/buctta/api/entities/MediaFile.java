package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 上传文件的元数据。
 * <p>
 * 文件本体不落库，按 {@link #storageKey} 存放在 {@code app.media.root} 指向的目录下，
 * 通过 {@code /api/media/{id}/content} 读取。
 */
@Entity
@Table(name = "media_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 磁盘上的相对存储路径，形如 {@code 2026/02/0f9c...-8a1b.mp4} */
    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    /** 用户上传时的原始文件名，仅用于展示与下载命名 */
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /** 经过内容嗅探后确定的 MIME 类型 */
    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private MediaKind kind;

    @Column(name = "created_time", insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdTime;

    public MediaFile(String storageKey, String originalFilename, String contentType,
                     Long fileSize, MediaKind kind) {
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.kind = kind;
    }

    /** 视频可在浏览器中直接拖动播放，图片可直接用于 img/背景图 */
    public boolean isPlayable() {
        return kind == MediaKind.VIDEO;
    }
}
