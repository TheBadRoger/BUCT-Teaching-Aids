package com.buctta.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 媒体上传与播放相关配置，对应 {@code app.media.*}。
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.media")
public class MediaProperties {

    /**
     * 上传文件的落盘根目录。
     * 相对路径按进程工作目录解析，生产环境建议配置为容器内挂载卷的绝对路径。
     * 实际媒体目录为 {@code <root>/<storage>}。
     */
    private String root = "data/uploads";

    /** 媒体子目录名，用于与业务上传目录隔离 */
    private String storage = "media";

    /** 流式读取的缓冲区大小（字节） */
    private int streamBufferSize = 64 * 1024;

    /** 播放地址前缀；留空时使用 {@code /api/media/<id>/content} */
    private String publicUrlPrefix = "";

    private Upload upload = new Upload();

    @Getter
    @Setter
    public static class Upload {

        /**
         * 视频大小上限（字节），默认 2GB。
         * 注意仍需同时满足 {@code spring.servlet.multipart.max-file-size}，两者取小。
         */
        private long maxVideoSize = 2L * 1024 * 1024 * 1024;

        /** 图片大小上限（字节），默认 20MB */
        private long maxImageSize = 20L * 1024 * 1024;

        /**
         * 读取文件头做类型嗅探的字节数。
         * 32 字节足以覆盖 jpg/png/gif/webp/ftyp/EBML 全部签名。
         */
        private int sniffLength = 32;

        private Set<String> allowedImageExtensions =
                new LinkedHashSet<>(java.util.List.of("jpg", "jpeg", "png", "gif", "webp"));

        private Set<String> allowedVideoExtensions =
                new LinkedHashSet<>(java.util.List.of("mp4", "webm", "mov", "mkv"));
    }
}
