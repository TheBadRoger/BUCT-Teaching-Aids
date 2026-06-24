package com.buctta.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 * <p>
 * BCrypt 特性：
 * 1. 慢哈希算法 - 通过 strength 参数控制计算复杂度，防止暴力破解
 * 2. 自动加盐 - 每次加密都会生成随机盐值，盐值包含在最终的哈希结果中
 * 3. 抗彩虹表攻击 - 由于加盐，预计算攻击无效
 * <p>
 * strength 参数说明：
 * - 默认值为 10
 * - 值越大，计算时间越长（每增加1，时间翻倍）
 * - 推荐值：12-14（生产环境）
 * - 值为 12 时，单次哈希约需 250ms
 * - 值为 14 时，单次哈希约需 1s
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 强度参数
     * 12 是推荐的安全强度，提供了良好的安全性和性能平衡
     */
    private static final int BCRYPT_STRENGTH = 12;

    /**
     * 密码编码器 Bean
     * 使用 BCrypt 慢哈希算法，自动加盐
     * <p>
     * 加密后的密码格式：$2a$12$[22字符的盐][31字符的哈希]
     * 例如：$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
     *
     * @return PasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}

