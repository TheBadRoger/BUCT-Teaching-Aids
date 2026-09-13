package com.buctta.api.controller;

import com.buctta.api.service.ExternalAIHandbookService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import com.buctta.api.utils.FileContentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;

/**
 * 背诵手册生成接口。
 * <p>
 * 上传 .txt / .docx / .pdf 学习资料，返回 AI 生成的背诵手册 JSON。
 * 该接口受登录保护（不在 Security 白名单内），未登录会跳转登录页。
 */
@Slf4j
@RestController
@RequestMapping("/api/handbook")
@RequiredArgsConstructor
public class HandbookCtrl {

    private final ExternalAIHandbookService handbookService;

    @PostMapping("/generate")
    public ApiResponse<JsonNode> generate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "topicName", required = false, defaultValue = "") String topicName) {

        if (file == null || file.isEmpty()) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "file");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !isSupported(fileName)) {
            return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR.getCode(), "仅支持 .txt / .docx / .pdf 文件");
        }

        try {
            String content = extractContent(file);
            if (content == null || content.isBlank()) {
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR.getCode(), "文件内容为空，请检查文件");
            }

            JsonNode handbook = handbookService.generate(content, topicName);
            return ApiResponse.ok(handbook);
        } catch (Exception e) {
            log.error("生成背诵手册失败: fileName={}", fileName, e);
            return ApiResponse.fail(BusinessStatus.EXTERNAL_API_ERROR.getCode(), "生成失败：" + e.getMessage());
        }
    }

    private boolean isSupported(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".docx") || lower.endsWith(".pdf");
    }

    private String extractContent(MultipartFile file) throws Exception {
        String lower = file.getOriginalFilename().toLowerCase();
        if (lower.endsWith(".txt")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } else if (lower.endsWith(".docx")) {
            return FileContentExtractor.parseDocxByIS(file.getInputStream());
        } else {
            return FileContentExtractor.parsePdfByIS(file.getInputStream());
        }
    }
}
