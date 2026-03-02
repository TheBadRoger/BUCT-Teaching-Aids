package com.buctta.api.service;

/**
 * 网络身份认证服务接口
 * 用于验证用户的真实身份信息（姓名、身份证号、学号/工号）
 * <p>
 * 配置身份认证服务提供商时，请实现此接口并配置相应的属性：
 * <p>
 * 1. 学信网认证（学生）:
 * - 在 application.properties 中添加:
 * identity.chsi.api-url=https://api.chsi.com.cn/verify
 * identity.chsi.app-id=你的AppId
 * identity.chsi.app-secret=你的AppSecret
 * <p>
 * 2. 公安部身份认证:
 * - 在 application.properties 中添加:
 * identity.police.api-url=https://api.police.cn/verify
 * identity.police.app-id=你的AppId
 * identity.police.app-secret=你的AppSecret
 * <p>
 * 3. 第三方实名认证服务（如阿里云、腾讯云）:
 * - 阿里云实名认证:
 * identity.aliyun.access-key-id=你的AccessKeyId
 * identity.aliyun.access-key-secret=你的AccessKeySecret
 * - 腾讯云实名认证:
 * identity.tencent.secret-id=你的SecretId
 * identity.tencent.secret-key=你的SecretKey
 */
public interface IdentityVerificationService {

    /**
     * 验证学生身份
     *
     * @param name          姓名
     * @param idCard        身份证号
     * @param studentNumber 学号
     * @return 验证结果
     */
    VerificationResult verifyStudent(String name, String idCard, String studentNumber);

    /**
     * 验证教师身份
     *
     * @param name           姓名
     * @param idCard         身份证号
     * @param employeeNumber 工号
     * @return 验证结果
     */
    VerificationResult verifyTeacher(String name, String idCard, String employeeNumber);

    /**
     * 身份验证结果
     */
    record VerificationResult(
            boolean success,
            String message,
            VerifiedInfo verifiedInfo
    ) {
        public static VerificationResult success(VerifiedInfo info) {
            return new VerificationResult(true, "验证成功", info);
        }

        public static VerificationResult fail(String message) {
            return new VerificationResult(false, message, null);
        }
    }

    /**
     * 已验证的身份信息
     */
    record VerifiedInfo(
            String name,           // 姓名
            String gender,         // 性别
            String organization,   // 所属机构/学校
            String department,     // 院系
            String className,      // 班级（学生）
            String education,      // 学历
            String admissionDate   // 入学日期（学生）/入职日期（教师）
    ) {
    }
}

