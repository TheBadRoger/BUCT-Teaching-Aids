package com.buctta.api.controller;

import com.buctta.api.service.TeacherAIService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher-ai")
public class TeacherAICtrl {
    private final TeacherAIService service;
    public TeacherAICtrl(TeacherAIService service) { this.service = service; }

    @GetMapping("/health")
    public Map<String, Object> health() { return service.health(); }

    @GetMapping("/knowledge/subjects")
    public Map<String, Object> subjects() throws IOException { return Map.of("success", true, "data", service.catalog()); }

    @PostMapping("/knowledge/generate")
    public Map<String, Object> knowledge(@RequestBody Map<String, Object> input) throws IOException {
        return Map.of("success", true, "data", service.generate(input, false));
    }

    @PostMapping("/lesson-plan/generate")
    public Map<String, Object> lesson(@RequestBody Map<String, Object> input) throws IOException {
        return Map.of("success", true, "data", service.generate(input, true));
    }

    @PostMapping("/lesson-plan/generate-stream")
    public void stream(@RequestBody Map<String, Object> input, HttpServletResponse response) throws IOException {
        service.validate(input);
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        var output = response.getOutputStream();
        try { service.stream(input, output); }
        catch (TeacherAIService.AIException ex) { service.event(output, Map.of("type", "error", "message", ex.getMessage())); }
        catch (IOException ex) { service.event(output, Map.of("type", "error", "message", "模型连接中断或超时，请重试")); }
    }

    @ExceptionHandler(TeacherAIService.AIException.class)
    public ResponseEntity<?> modelError(TeacherAIService.AIException ex) {
        return ResponseEntity.status(ex.status).body(Map.of("success", false, "error", ex.getMessage(), "type", ex.type));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> connectionError(IOException ex) {
        return ResponseEntity.status(ex instanceof SocketTimeoutException ? 504 : 502)
                .body(Map.of("success", false, "error", "模型连接失败或超时，请稍后重试", "type", "network"));
    }
}
