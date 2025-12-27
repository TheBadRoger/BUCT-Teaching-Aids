package com.buctta.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityAuthorize {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //授权规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/enter.html",
                                "/register.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/api/aijudegment/login",
                                "/api/aijudegment/register"
                        )
                        .permitAll()
                        .anyRequest().authenticated()
                )
                //关闭表单验证
                .formLogin(AbstractHttpConfigurer::disable)
                //持久化
                .securityContext(context -> context
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                )
                //异常处理：未授权时自动跳转到登录页面
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/enter.html"))
                )
                //Session 管理：超时时自动跳转到登录页面
                .sessionManagement(session -> session
                        .invalidSessionUrl("/enter.html")
                        .sessionFixationProtection(org.springframework.security.config.http.SessionFixationProtectionStrategy.NONE)
                )
                //关闭 CSRF
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}