package com.buctta.api.dto;

import lombok.Data;

/**
 * 用户登录请求DTO
 */
@Data
public class LoginRequest {

    /**
     * 登录方式: PASSWORD, SMS_CODE, EMAIL_CODE
     */
    private LoginType loginType;

    /**
     * 用户名 (密码登录时使用)
     */
    private String username;

    /**
     * 密码 (密码登录时使用)
     */
    private String password;

    /**
     * 手机号 (手机验证码登录时使用)
     */
    private String telephone;

    /**
     * 邮箱 (邮箱验证码登录时使用)
     */
    private String email;

    /**
     * 验证码 (验证码登录时使用)
     */
    private String code;

    public enum LoginType {
        PASSWORD,      // 用户名+密码
        SMS_CODE,      // 手机号+验证码
        EMAIL_CODE     // 邮箱+验证码
    }
}

