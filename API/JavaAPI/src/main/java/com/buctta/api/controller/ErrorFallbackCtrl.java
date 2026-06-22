package com.buctta.api.controller;

import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class ErrorFallbackCtrl implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<?> handleError(HttpServletRequest request) {
        int statusCode = resolveStatusCode(request);
        String accept = request.getHeader(HttpHeaders.ACCEPT);

        if (accept != null && accept.contains(MediaType.TEXT_HTML_VALUE)) {
            return ResponseEntity.status(statusCode)
                    .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                    .body(renderErrorHtml(statusCode));
        }

        BusinessStatus businessStatus = (statusCode == 404)
                ? BusinessStatus.RESOURCE_NOT_FOUND
                : BusinessStatus.INTERNAL_ERROR;

        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(businessStatus));
    }

    private String renderErrorHtml(int statusCode) {
        String statusText = resolveStatusText(statusCode);
        String fallbackHtml = "<html><body><h2>请求处理失败</h2><p>状态码：" + statusCode
                + " - " + statusText + "</p></body></html>";

        try {
            ClassPathResource resource = new ClassPathResource("static/error.html");
            byte[] bytes = resource.getInputStream().readAllBytes();
            String template = new String(bytes, StandardCharsets.UTF_8);
            return template
                    .replace("{{STATUS_CODE}}", String.valueOf(statusCode))
                    .replace("{{STATUS_TEXT}}", statusText);
        } catch (IOException ignored) {
            return fallbackHtml;
        }
    }

    private String resolveStatusText(int statusCode) {
        return switch (statusCode) {
            case 400 -> "请求参数错误";
            case 401 -> "未授权访问";
            case 403 -> "禁止访问";
            case 404 -> "资源不存在";
            case 500 -> "服务器内部错误";
            default -> "请求异常";
        };
    }

    private int resolveStatusCode(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (code instanceof Integer value && value >= 100 && value <= 599) {
            return value;
        }
        if (code instanceof String value) {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed >= 100 && parsed <= 599) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // Fall through to default status.
            }
        }
        return 500;
    }
}





