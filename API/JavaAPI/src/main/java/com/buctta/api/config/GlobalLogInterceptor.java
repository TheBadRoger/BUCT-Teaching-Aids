package com.buctta.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class GlobalLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GlobalLogInterceptor.class);
    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        long start = System.currentTimeMillis();
        request.setAttribute(START_TIME, start);

        log.info(">>> {} {} from {} {}",
                request.getMethod(),
                request.getRequestURI(),
                getClientIp(request),
                handler.getClass().getSimpleName());

        return true;          // 返回 false 就中断请求
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           @Nullable ModelAndView modelAndView) {

        Long start = (Long) request.getAttribute(START_TIME);
        if (start != null) {
            long cost = System.currentTimeMillis() - start;
            log.info("<<< {} {} cost {} ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    cost);
        }
    }

    /* 简单取 IP，支持 nginx 反向代理 */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}