package com.buctta.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用自定义配置属性
 * <p>
 * 用于管理开发/生产环境的功能开关
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * 验证码服务配置
     */
    private VerificationCode verificationCode = new VerificationCode();

    /**
     * 身份认证服务配置
     */
    private IdentityVerification identityVerification = new IdentityVerification();

    /**
     * 外部 AI（polymas）服务配置
     */
    private Ai ai = new Ai();

    @Getter
    @Setter
    public static class VerificationCode {
        /**
         * 是否使用模拟模式
         * true: 验证码不会真正发送，仅在日志中显示（开发环境）
         * false: 使用真实的短信/邮件服务发送验证码（生产环境）
         */
        private boolean mockMode = true;
    }

    @Getter
    @Setter
    public static class IdentityVerification {
        /**
         * 是否使用模拟模式
         * true: 身份验证直接返回成功（开发环境）
         * false: 调用真实的身份认证服务（生产环境）
         */
        private boolean mockMode = true;
    }

    @Getter
    @Setter
    public static class Ai {
        private Polymas polymas = new Polymas();
    }

    /**
     * polymas 第三方 AI 平台配置
     * <p>
     * ⚠️ authKey 是敏感密钥，切勿提交到公开仓库。
     * 强烈建议通过环境变量 POLYMAS_AUTH_KEY（或 .env）注入，
     * 并在 polymas 后台重置已被泄露的密钥。
     */
    @Getter
    @Setter
    public static class Polymas {
        private String endpoint = "https://cloudapi.polymas.com/bot/v2/completions/chat/stream";
        private String authKey = "";
        private String stopMark = "(AI生成)";
        private String appCode = "ti39Ohdy6k";
        private String userNid = "N7I7pSTrm3";
        private String sessionNid = "PNeg1BjP6x";
        private String chatNid = "JJdylJaMSF";
    }
}

