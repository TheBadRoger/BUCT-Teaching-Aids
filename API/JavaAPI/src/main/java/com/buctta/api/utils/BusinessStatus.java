package com.buctta.api.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.MessageFormat;

@Getter
@AllArgsConstructor
public enum BusinessStatus {
    // 2xxx 成功
    SUCCESS(2000, "Ok."),

    // 400x 参数错误
    PARAM_MISSING(4001, "Parameters not found: {0}."),
    PARAM_TYPE_ERROR(4002, "Parameters type incorrect: {0}."),
    PARAM_FORMAT_ERROR(4003, "Parameters format incorrect."),

    // 401x 认证错误
    ACCOUNT_PASSWORD_ERROR(4011, "Incorrect password or name."),
    TOKEN_EXPIRED(4012, "Login expired."),
    TOKEN_INVALID(4013, "Invalid token."),
    VERIFICATION_CODE_ERROR(4014, "Invalid or expired verification code."),
    SEND_CODE_FAILED(4015, "Failed to send verification code."),

    // 403x 权限错误
    NO_PERMISSION(4031, "No permission."),
    ACCOUNT_LOCKED(4032, "Account locked."),

    // 404x 不存在
    USER_NOT_FOUND(4041, "User not found."),
    RESOURCE_NOT_FOUND(4042, "Resource not found."),

    // 409x 冲突
    ENTITY_EXISTS(4091, "Entity already exists."),
    USERNAME_EXISTS(4092, "Username already exists."),
    EMAIL_EXISTS(4093, "Email already exists."),
    PHONE_EXISTS(4094, "Phone already exists."),
    ALREADY_BOUND(4095, "User already bound to an identity."),
    IDENTITY_ALREADY_BOUND(4096, "This identity is already bound to another user."),
    NOT_BOUND(4097, "User has not bound any identity."),
    BINDING_CONFLICT(4098, "Cannot bind this identity type, user already has another identity type bound."),

    // 410x 身份验证错误
    IDENTITY_VERIFY_FAILED(4101, "Identity verification failed: {0}."),

    // 5xxx 服务器错误
    INTERNAL_ERROR(5000, "Internal system error."),
    DATABASE_ERROR(5001, "Database error."),
    REDIS_ERROR(5002, "Cache error."),
    EXTERNAL_API_ERROR(5003, "External API error.");

    private final int code;
    private final String template;

    public String format(Object... args) {
        return args.length == 0 ? template : MessageFormat.format(template, args);
    }
}
