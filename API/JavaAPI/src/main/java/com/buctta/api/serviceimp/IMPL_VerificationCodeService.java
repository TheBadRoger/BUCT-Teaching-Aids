package com.buctta.api.serviceimp;

import com.buctta.api.config.AppProperties;
import com.buctta.api.service.VerificationCodeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现
 * <p>
 * 根据配置 app.verification-code.mock-mode 决定运行模式：
 * - true (开发模式): 验证码存储在Redis但不真正发送，在日志中显示
 * - false (生产模式): 调用真实的短信/邮件服务发送验证码
 */
@Slf4j
@Service
public class IMPL_VerificationCodeService implements VerificationCodeService {

    // 验证码有效期（分钟）
    private static final int CODE_EXPIRE_MINUTES = 5;
    // Redis key 前缀
    private static final String SMS_CODE_PREFIX = "verification:sms:";
    private static final String EMAIL_CODE_PREFIX = "verification:email:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AppProperties appProperties;

    /**
     * 生成6位随机验证码
     */
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    @Override
    public SendResult sendSmsCode(String telephone) {
        try {
            String code = generateCode();

            // 将验证码存储到 Redis，设置过期时间
            String key = SMS_CODE_PREFIX + telephone;
            stringRedisTemplate.opsForValue().set(key, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            if (appProperties.getVerificationCode().isMockMode()) {
                // 开发模式：仅记录日志，不真正发送
                log.info("【开发模式】短信验证码已生成 - 手机号: {}, 验证码: {}", telephone, code);
                log.warn("【注意】当前为模拟发送，生产环境请设置 app.verification-code.mock-mode=false");
            }
            else {
                // 生产模式：调用真实短信服务
                // TODO: 接入真实的短信服务提供商
                log.info("【生产模式】发送短信验证码 - 手机号: {}", telephone);
            }

            return SendResult.ok("验证码发送成功");
        }
        catch (Exception e) {
            log.error("发送短信验证码失败: {}", e.getMessage(), e);
            return SendResult.fail("SEND_FAILED", "发送短信验证码失败: " + e.getMessage());
        }
    }

    @Override
    public SendResult sendEmailCode(String email) {
        try {
            String code = generateCode();

            // 将验证码存储到 Redis，设置过期时间
            String key = EMAIL_CODE_PREFIX + email;
            stringRedisTemplate.opsForValue().set(key, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            if (appProperties.getVerificationCode().isMockMode()) {
                // 开发模式：仅记录日志，不真正发送
                log.info("【开发模式】邮箱验证码已生成 - 邮箱: {}, 验证码: {}", email, code);
                log.warn("【注意】当前为模拟发送，生产环境请设置 app.verification-code.mock-mode=false");
            }
            else {
                // 生产模式：调用真实邮件服务
                // TODO: 接入真实的邮件服务
                log.info("【生产模式】发送邮箱验证码 - 邮箱: {}", email);
            }

            return SendResult.ok("验证码发送成功");
        }
        catch (Exception e) {
            log.error("发送邮箱验证码失败: {}", e.getMessage(), e);
            return SendResult.fail("SEND_FAILED", "发送邮箱验证码失败: " + e.getMessage());
        }
    }

    @Override
    public VerifyResult verifySmsCode(String telephone, String code) {
        try {
            String key = SMS_CODE_PREFIX + telephone;
            String storedCode = stringRedisTemplate.opsForValue().get(key);

            if (storedCode == null) {
                return VerifyResult.fail("CODE_EXPIRED", "验证码已过期或不存在");
            }
            if (!storedCode.equals(code)) {
                return VerifyResult.fail("CODE_MISMATCH", "验证码错误");
            }
            // 验证成功后删除验证码
            stringRedisTemplate.delete(key);
            return VerifyResult.ok();
        }
        catch (Exception e) {
            log.error("验证短信验证码失败: {}", e.getMessage(), e);
            return VerifyResult.fail("VERIFY_FAILED", "验证失败: " + e.getMessage());
        }
    }

    @Override
    public VerifyResult verifyEmailCode(String email, String code) {
        try {
            String key = EMAIL_CODE_PREFIX + email;
            String storedCode = stringRedisTemplate.opsForValue().get(key);

            if (storedCode == null) {
                return VerifyResult.fail("CODE_EXPIRED", "验证码已过期或不存在");
            }
            if (!storedCode.equals(code)) {
                return VerifyResult.fail("CODE_MISMATCH", "验证码错误");
            }
            // 验证成功后删除验证码
            stringRedisTemplate.delete(key);
            return VerifyResult.ok();
        }
        catch (Exception e) {
            log.error("验证邮箱验证码失败: {}", e.getMessage(), e);
            return VerifyResult.fail("VERIFY_FAILED", "验证失败: " + e.getMessage());
        }
    }
}
