package com.buctta.api.utils;

/**
 * 媒体地址安全校验。
 * <p>
 * 图片、附件地址会直接进入 {@code <img src>} / {@code <a href>}，如果放任
 * {@code javascript:}、{@code data:} 等协议入库，渲染时就变成 XSS 载荷。
 * 因此写库前统一只放行两类值：站内相对路径与 http(s) 绝对地址。
 */
public final class MediaUrls {

    private MediaUrls() {
    }

    /**
     * 是否为可安全使用的媒体地址。
     * <p>
     * 空值视为合法（表示"不设置"），由调用方决定是否必填。
     */
    public static boolean isSafe(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String trimmed = url.trim();
        return trimmed.startsWith("/")
                || trimmed.startsWith("http://")
                || trimmed.startsWith("https://");
    }

    /** 校验失败时的统一说明文案 */
    public static String rejectMessage(String field) {
        return field + " 只能是站内路径（以 / 开头）或 http(s) 地址，当前值不合法";
    }
}
