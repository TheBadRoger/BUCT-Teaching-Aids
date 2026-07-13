package com.buctta.api.dto;

import lombok.Data;

/**
 * 发送验证码请求DTO
 */
@Data
public class SendCodeRequest {

    /**
     * 发送类型: SMS, EMAIL
     */
    private SendType sendType;

    /**
     * 手机号 (发送短信验证码时使用)
     */
    private String telephone;

    /**
     * 邮箱 (发送邮箱验证码时使用)
     */
    private String email;

    public enum SendType {
        SMS,    // 短信验证码
        EMAIL   // 邮箱验证码
    }
}

