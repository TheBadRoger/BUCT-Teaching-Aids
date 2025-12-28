package com.buctta.api.controller;

import com.buctta.api.service.ExternalAIJudge;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String,String>> startJudge(
            @RequestParam List<String> extractedTexts,
            @RequestParam List<String> fileNames) {
        String id = aiGenerateService.submitTask(extractedTexts, fileNames);
        return ResponseEntity.ok(Map.of("id", id, "status", "started"));
    }

    @GetMapping("/generate/stream/{id}")
    public SseEmitter stream(@PathVariable String id) {
        return aiGenerateService.getEmitter(id);
    }
}