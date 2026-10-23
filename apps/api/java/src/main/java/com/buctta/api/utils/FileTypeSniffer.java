package com.buctta.api.utils;

import com.buctta.api.entities.MediaKind;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 通过读取文件头的魔术字节判断真实文件类型，避免仅凭扩展名判断导致
 * 伪造扩展名（例如把任意文件改名成 .mp4）被存下并对外提供。
 * <p>
 * 不依赖 Spring 容器，便于单元测试。
 */
public final class FileTypeSniffer {

    /** 空文件没有可判定的文件头 */
    public static final String TYPE_UNKNOWN = "application/octet-stream";

    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp"
    );

    private static final Map<String, String> VIDEO_TYPES = Map.of(
            "mp4", "video/mp4",
            "webm", "video/webm",
            "mov", "video/quicktime",
            "mkv", "video/x-matroska"
    );

    /** ftyp 家族里更常见的 brand → 精确 MIME 映射 */
    private static final Map<String, String> FTYP_BRANDS = Map.of(
            "qt  ", "video/quicktime",
            "isom", "video/mp4",
            "iso2", "video/mp4",
            "mp41", "video/mp4",
            "mp42", "video/mp4",
            "avc1", "video/mp4",
            "dash", "video/mp4",
            "M4V ", "video/x-m4v"
    );

    /** 浏览器无法原生播放的容器格式，上传成功后需要给出提示 */
    private static final Set<String> NON_BROWSER_PLAYABLE = Set.of("mkv", "avi", "wmv", "flv");

    private FileTypeSniffer() {
    }

    /**
     * 解析上传文件的真实类型。
     * <p>
     * 注意：本方法以 {@code extension} 作为 ftyp 家族内部的消歧依据
     * （mp4 与 mov 的文件头完全一致，只能靠扩展名区分），因此调用方仍需
     * 校验扩展名是否在白名单内，两者结合才是完整的校验。
     *
     * @param header    文件头字节
     * @param length    有效长度
     * @param extension 小写扩展名（不含点），可为 null
     */
    public static String resolveContentType(byte[] header, int length, String extension) {
        if (header == null || length < 4) {
            return TYPE_UNKNOWN;
        }

        // ---- 图片：签名唯一，无需扩展名辅助 ----
        if (startsWith(header, length, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return IMAGE_TYPES.get("jpg");
        }
        if (startsWith(header, length, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})) {
            return IMAGE_TYPES.get("png");
        }
        if (startsWith(header, length, new byte[]{'G', 'I', 'F', '8'})) {
            return IMAGE_TYPES.get("gif");
        }
        // WebP: "RIFF" + 4 字节长度 + "WEBP"
        if (startsWith(header, length, new byte[]{'R', 'I', 'F', 'F'})
                && length >= 12
                && startsWithAt(header, new byte[]{'W', 'E', 'B', 'P'}, 8)) {
            return IMAGE_TYPES.get("webp");
        }

        // ---- 视频：ISO BMFF 容器（mp4 / mov）----
        if (length >= 12 && startsWithAt(header, new byte[]{'f', 't', 'y', 'p'}, 4)) {
            String brand = new String(header, 8, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
            String precise = FTYP_BRANDS.get(brand);
            if (precise != null) {
                return precise;
            }
            // 未知 brand：mp4 与 mov 头部相同，用扩展名消歧，默认按 mp4 处理
            if ("mov".equals(extension)) {
                return VIDEO_TYPES.get("mov");
            }
            return VIDEO_TYPES.get("mp4");
        }
        // WebM / Matroska 共用 EBML 头
        if (startsWith(header, length, new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3})) {
            if ("webm".equals(extension)) {
                return VIDEO_TYPES.get("webm");
            }
            if ("mkv".equals(extension)) {
                return VIDEO_TYPES.get("mkv");
            }
            return VIDEO_TYPES.get("webm");
        }
        // AVI: "RIFF" + 4 字节长度 + "AVI "
        if (startsWith(header, length, new byte[]{'R', 'I', 'F', 'F'})
                && length >= 12
                && startsWithAt(header, new byte[]{'A', 'V', 'I', ' '}, 8)) {
            return "video/x-msvideo";
        }

        return TYPE_UNKNOWN;
    }

    /** 由 MIME 反推文件种类，未知类型归为 null */
    public static MediaKind kindOf(String contentType) {
        if (contentType == null || TYPE_UNKNOWN.equals(contentType)) {
            return null;
        }
        if (contentType.startsWith("image/")) {
            return MediaKind.IMAGE;
        }
        if (contentType.startsWith("video/")) {
            return MediaKind.VIDEO;
        }
        return null;
    }

    /** 该 MIME 是否属于浏览器通常无法直接播放的容器 */
    public static boolean isLikelyNotPlayableInBrowser(String contentType) {
        return "video/x-msvideo".equals(contentType)
                || VIDEO_TYPES.get("mkv").equals(contentType)
                || "video/x-ms-wmv".equals(contentType)
                || "video/x-flv".equals(contentType);
    }

    /** 去掉目录与危险字符，只保留扩展名（不含点，小写） */
    public static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String base = slash >= 0 ? filename.substring(slash + 1) : filename;
        int dot = base.lastIndexOf('.');
        if (dot < 0 || dot == base.length() - 1) {
            return "";
        }
        return base.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean startsWith(byte[] data, int length, byte[] magic) {
        return startsWithAt(data, magic, 0);
    }

    private static boolean startsWithAt(byte[] data, byte[] magic, int offset) {
        if (data.length < offset + magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[offset + i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
