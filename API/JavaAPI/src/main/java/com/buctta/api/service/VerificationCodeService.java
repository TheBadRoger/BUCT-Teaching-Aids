package com.buctta.api.service;

/**
 * 验证码服务接口
 * 用于发送和验证手机/邮箱验证码
 * <p>
 * 配置验证码服务提供商时，请实现此接口并配置相应的属性：
 * <p>
 * 1. 阿里云短信服务:
 * - 在 application.properties 中添加:
 * sms.aliyun.access-key-id=你的AccessKeyId
 * sms.aliyun.access-key-secret=你的AccessKeySecret
 * sms.aliyun.sign-name=你的短信签名
 * sms.aliyun.template-code=你的模板代码
 * - 添加依赖: com.aliyun:aliyun-java-sdk-dysmsapi
 * <p>
 * 2. 腾讯云短信服务:
 * - 在 application.properties 中添加:
 * sms.tencent.secret-id=你的SecretId
 * sms.tencent.secret-key=你的SecretKey
 * sms.tencent.app-id=你的AppId
 * sms.tencent.sign-name=你的短信签名
 * sms.tencent.template-id=你的模板ID
 * - 添加依赖: com.tencentcloudapi:tencentcloud-sdk-java
 * <p>
 * 3. 邮箱验证码 (使用 Spring Mail):
 * - 在 application.properties 中添加:
 * spring.mail.host=smtp.example.com
 * spring.mail.port=587
 * spring.mail.username=your-email@example.com
 * spring.mail.password=your-password
 * spring.mail.properties.mail.smtp.auth=true
 * spring.mail.properties.mail.smtp.starttls.enable=true
 * - 添加依赖: org.springframework.boot:spring-boot-starter-mail
 */
public interface VerificationCodeService {

    /**
     * 发送手机验证码
     *
     * @param telephone 手机号
     * @return 发送结果
     */
    SendResult sendSmsCode(String telephone);

    /**
     * 发送邮箱验证码
     *
     * @param email 邮箱地址
     * @return 发送结果
     */
    SendResult sendEmailCode(String email);

    /**
     * 验证手机验证码
     *
     * @param telephone 手机号
     * @param code      验证码
     * @return 验证结果
     */
    VerifyResult verifySmsCode(String telephone, String code);

    /**
     * 验证邮箱验证码
     *
     * @param email 邮箱地址
     * @param code  验证码
     * @return 验证结果
     */
    VerifyResult verifyEmailCode(String email, String code);

    /**
     * 发送结果
     */
    record SendResult(boolean success, String errorCode, String message) {
        public static SendResult ok() {
            return new SendResult(true, null, "验证码发送成功");
        }

        public static SendResult ok(String message) {
            return new SendResult(true, null, message);
        }

        public static SendResult fail(String errorCode, String message) {
            return new SendResult(false, errorCode, message);
        }
    }

    /**
     * 验证结果
     */
    record VerifyResult(boolean success, String errorCode, String message) {
        public static VerifyResult ok() {
            return new VerifyResult(true, null, "验证成功");
        }

        public static VerifyResult fail(String errorCode, String message) {
            return new VerifyResult(false, errorCode, message);
        }
    }
}
