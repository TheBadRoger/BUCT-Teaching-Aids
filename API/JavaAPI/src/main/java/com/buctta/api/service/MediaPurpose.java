package com.buctta.api.service;

import com.buctta.api.entities.MediaKind;

/**
 * 上传意图。
 * <p>
 * 同一个上传接口通过该枚举区分「这份文件是干什么用的」：既是文件类型校验的依据
 * （图片用途拒绝视频，反之亦然），也决定上传成功后要回填哪个业务字段。
 */
public enum MediaPurpose {

    /** 课程视频本体，落库为 course_video，不直接回填字段 */
    VIDEO(MediaKind.VIDEO, "courseVideo"),

    /** 课程封面图，回填 course_list.course_image */
    COURSE_COVER(MediaKind.IMAGE, "courseImage"),

    /** 机构 logo，回填 organization_list.logo */
    ORG_LOGO(MediaKind.IMAGE, "logo"),

    /** 机构 banner，回填 organization_list.banner_url */
    ORG_BANNER(MediaKind.IMAGE, "bannerUrl"),

    /** 机构荣誉证书，回填 organization_list.honor_cert_url */
    ORG_HONOR_CERT(MediaKind.IMAGE, "honorCertUrl");

    private final MediaKind requiredKind;
    private final String fieldName;

    MediaPurpose(MediaKind requiredKind, String fieldName) {
        this.requiredKind = requiredKind;
        this.fieldName = fieldName;
    }

    /** 该用途只接受的文件种类 */
    public MediaKind requiredKind() {
        return requiredKind;
    }

    /** 上传成功后回填的业务字段名，便于前端/日志对照 */
    public String fieldName() {
        return fieldName;
    }

    public boolean isImage() {
        return requiredKind == MediaKind.IMAGE;
    }

    /** 忽略大小写与首尾空白的解析，无法识别时返回 null */
    public static MediaPurpose parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
}
