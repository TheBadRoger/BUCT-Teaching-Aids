package com.buctta.api.controller;

import com.buctta.api.service.ExternalAIJudge;
import com.buctta.api.utils.ApiResponse;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ExternalAIGenerateCtrl {
    @Resource
    private final ExternalAIJudge aiGenerateService;

    @PostMapping("/generate/start")
    public ApiResponse<Map<String, String>> startJudge(
            @RequestParam List<String> extractedTexts,
            @RequestParam List<String> fileNames,
            @RequestParam int counts) {
        if (counts == 1) {
            List<String> tmp = new java.util.ArrayList<>();
            StringBuilder t = new StringBuilder(extractedTexts.getFirst());
            for (int i = 1; i < extractedTexts.size(); i++) {
                t.append(extractedTexts.get(i));
            }
            tmp.add(t.toString());

            String id = aiGenerateService.submitTask(tmp, fileNames);
            return ApiResponse.ok(Map.of("id", id, "status", "started"));
        }
        else {
            String id = aiGenerateService.submitTask(extractedTexts, fileNames);
            return ApiResponse.ok(Map.of("id", id, "status", "started"));
        }
    }

    @GetMapping("/generate/stream/{id}")
    public SseEmitter stream(@PathVariable String id) {
        return aiGenerateService.getEmitter(id);
    }
}