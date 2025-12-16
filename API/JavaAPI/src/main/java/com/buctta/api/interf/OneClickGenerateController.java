package com.buctta.api.interf;

import com.buctta.api.service.ExternalAIJudge;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class OneClickGenerateController {

    private final ExternalAIJudge aiGenerateService;

    @PostMapping("/generate/start")
    public ResponseEntity<Map<String,String>> start(
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