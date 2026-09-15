package com.buctta.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityAuthorize {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(
                                        // 登录 / 注册 / 错误页
                                        "/enter.html",
                                        "/register.html",
                                        "/login.html",
                                        "/error",
                                        // 公共静态资源
                                        "/css/**",
                                        "/js/**",
                                        "/images/**",
                                        // 学生智慧前台页面（页面本身放行，页面内调用的 /api/** 仍需登录）
                                        "/index.html",
                                        "/search.html",
                                        "/course-list.html",
                                        "/course-info.html",
                                        "/course-play.html",
                                        "/interactive-play.html",
                                        "/guessyouneed.html",
                                        "/ai-agent.html",
                                        "/AIchat.html",
                                        "/institution.html",
                                        // AI 背诵手册页面及其自带的 css/js（页面本身放行，
                                        // 页面内调用的 /api/handbook/** 仍需登录）
                                        "/recitation/**",
                                        // 认证与绑定接口
                                        "/api/aijudegment/login",
                                        "/api/aijudegment/register",
                                        "/api/admin/login",
                                        "/api/admin/register",
                                        "/api/user/auth/login",
                                        "/api/user/auth/register",
                                        "/api/user/auth/send-code"
                                )
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )
                //关闭表单验证
                .formLogin(AbstractHttpConfigurer::disable)
                //持久化
                .securityContext(context -> context
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                )
                //会话策略：按会话空闲时间失效，失效后跳转登录页
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/enter.html")
                )
                //异常处理：未授权时自动跳转到登录页面
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/enter.html"))
                )
                //关闭 CSRF
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}