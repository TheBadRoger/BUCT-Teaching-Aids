package com.buctta.api.service;

import com.buctta.api.utils.BusinessStatus;

/**
 * 媒体校验失败异常。
 * <p>
 * 携带 {@link BusinessStatus} 与可读原因，由 Controller 统一翻译成
 * {@code ApiResponse.fail(code, msg)}，避免把校验细节泄漏成 500。
 */
public class MediaValidationException extends RuntimeException {

    private final BusinessStatus status;

    public MediaValidationException(BusinessStatus status, String message) {
        super(message);
        this.status = status;
    }

    public MediaValidationException(BusinessStatus status) {
        this(status, status.getTemplate());
    }

    public BusinessStatus status() {
        return status;
    }
}
