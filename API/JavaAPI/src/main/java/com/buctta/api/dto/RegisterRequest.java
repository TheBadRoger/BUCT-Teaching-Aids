package com.buctta.api.dto;

import lombok.Data;

/**
 * 用户注册请求DTO
 * 注册时不需要指定用户类型，绑定身份时自动设置
 */
@Data
public class RegisterRequest {

    /**
     * 注册方式: SMS_CODE, EMAIL_CODE
     */
    private RegisterType registerType;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 手机号 (手机验证码注册时使用)
     */
    private String telephone;

    /**
     * 邮箱 (邮箱验证码注册时使用)
     */
    private String email;

    /**
     * 验证码
     */
    private String code;

    public enum RegisterType {
        SMS_CODE,      // 手机号+验证码
        EMAIL_CODE     // 邮箱+验证码
    }
}

