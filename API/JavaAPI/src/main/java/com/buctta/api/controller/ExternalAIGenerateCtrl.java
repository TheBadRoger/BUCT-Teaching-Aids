package com.buctta.api.controller;

import com.buctta.api.service.ExternalAIJudgeService;
import com.buctta.api.utils.ApiResponse;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ExternalAIGenerateCtrl {
    @Resource
    private final ExternalAIJudgeService aiGenerateService;

    @PostMapping("/generate/start")
    public ApiResponse<Map<String, String>> startJudge(@RequestBody StartJudgeRequest request) {
        List<String> extractedTexts = request.getExtractedTexts();
        List<String> fileNames = request.getFileNames();

        String id = aiGenerateService.submitTask(extractedTexts, fileNames);
        return ApiResponse.ok(Map.of("id", id, "status", "started"));
    }

    @GetMapping("/generate/stream/{id}")
    public SseEmitter stream(@PathVariable String id) {
        return aiGenerateService.getEmitter(id);
    }

    @Getter
    @Setter
    public static class StartJudgeRequest {
        private List<String> extractedTexts;
        private List<String> fileNames;
        private int counts;
    }
}