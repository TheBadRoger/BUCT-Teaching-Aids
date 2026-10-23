package com.buctta.api.utils;

/**
 * HTTP {@code Range} 请求头的解析结果。
 * <p>
 * 仅支持最常用的单区间形式 {@code bytes=start-end}，包含三种写法：
 * <ul>
 *     <li>{@code bytes=0-499} —— 前 500 字节</li>
 *     <li>{@code bytes=500-} —— 从 500 字节到结尾</li>
 *     <li>{@code bytes=-500} —— 最后 500 字节</li>
 * </ul>
 * 视频播放器拖动进度条时依赖该机制，服务端必须返回
 * {@code 206 Partial Content} 与 {@code Content-Range}。
 */
public record ByteRange(long start, long end) {

    /** Range 头不合法或不可满足时返回 null，调用方按 200 / 416 处理 */
    public static ByteRange parse(String header, long fileSize) {
        if (header == null || fileSize <= 0) {
            return null;
        }
        String value = header.trim();
        if (!value.startsWith("bytes=")) {
            return null;
        }
        value = value.substring("bytes=".length()).trim();

        // 多区间（bytes=0-99,200-299）暂不支持，退化为整体响应
        if (value.contains(",")) {
            return null;
        }

        int dash = value.indexOf('-');
        if (dash < 0) {
            return null;
        }

        String rawStart = value.substring(0, dash).trim();
        String rawEnd = value.substring(dash + 1).trim();

        try {
            if (rawStart.isEmpty()) {
                // bytes=-N 取末尾 N 字节
                if (rawEnd.isEmpty()) {
                    return null;
                }
                long suffixLength = Long.parseLong(rawEnd);
                if (suffixLength <= 0) {
                    return null;
                }
                long start = Math.max(0, fileSize - suffixLength);
                return new ByteRange(start, fileSize - 1);
            }

            long start = Long.parseLong(rawStart);
            if (start < 0 || start >= fileSize) {
                // 起点越界，RFC 规定返回 416
                return null;
            }
            long end = rawEnd.isEmpty() ? fileSize - 1 : Long.parseLong(rawEnd);
            if (end < start) {
                return null;
            }
            // 请求超出文件末尾时截断到实际末尾
            end = Math.min(end, fileSize - 1);
            return new ByteRange(start, end);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    /** 区间长度（字节） */
    public long length() {
        return end - start + 1;
    }

    /** 用于 {@code Content-Range} 响应头，例如 {@code bytes 0-499/1234} */
    public String contentRange(long fileSize) {
        return "bytes " + start + "-" + end + "/" + fileSize;
    }
}
