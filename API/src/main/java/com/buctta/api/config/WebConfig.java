package com.buctta.api.config;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private GlobalLogInterceptor globalLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(globalLogInterceptor)
                .addPathPatterns("/**")          // 拦截所有请求
                .excludePathPatterns(            // 放行清单
                        "/error",
                        "/favicon.ico",
                        "/swagger-ui/**",
                        "/v3/api-docs/**");
    }
}