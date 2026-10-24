package com.buctta.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class TeacherAIService {
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeout;

    public TeacherAIService(ObjectMapper mapper,
            @Value("${app.teacher-ai.api-key:}") String apiKey,
            @Value("${app.teacher-ai.base-url:https://api.deepseek.com/v1}") String baseUrl,
            @Value("${app.teacher-ai.model:deepseek-chat}") String model,
            @Value("${app.teacher-ai.timeout:60000}") int timeout) {
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.timeout = Math.max(1, timeout);
    }

    public Map<String, Object> health() {
        return Map.of("status", "ok", "model", model, "hasApiKey", !apiKey.isBlank());
    }

    public JsonNode catalog() throws IOException {
        return mapper.readTree(resource("catalog.json"));
    }

    private String resource(String name) throws IOException {
        return new ClassPathResource("teacher-ai/" + name).getContentAsString(StandardCharsets.UTF_8);
    }

    public void validate(Map<String, Object> input) {
        var missing = new ArrayList<String>();
        for (String key : List.of("subject", "grade", "topic")) {
            if (!(input.get(key) instanceof String value) || value.isBlank()) missing.add(key);
        }
        if (!missing.isEmpty()) throw new AIException(400, "缺少必填参数: " + String.join(", ", missing), "validation");
        for (var entry : input.entrySet()) {
            if (entry.getKey().equals("duration")) {
                if (entry.getValue() != null && (!(entry.getValue() instanceof Number n)
                        || !Double.isFinite(n.doubleValue()) || n.doubleValue() <= 0 || n.doubleValue() > 1440))
                    throw new AIException(400, "课时必须为 1–1440 分钟", "validation");
            } else if (entry.getValue() != null && (!(entry.getValue() instanceof String s) || s.length() > 20000)) {
                throw new AIException(400, "参数必须为文本且不超过 20000 字符", "validation");
            }
        }
    }

    private Map<String, Object> payload(Map<String, Object> input, boolean lesson, boolean stream) throws IOException {
        String prompt = resource(lesson ? "lesson-prompt.txt" : "knowledge-prompt.txt").formatted(input.get("subject"));
        String user = "学科：" + input.get("subject") + "\n年级：" + input.get("grade")
                + (lesson ? "\n课题：" : "\n章节：") + input.get("topic");
        if (lesson) {
            user += "\n课型：" + Objects.requireNonNullElse(input.get("lessonType"), "新授课")
                    + "\n课时：" + Objects.requireNonNullElse(input.get("duration"), 45) + "分钟";
            if (input.get("teachingStyle") instanceof String s && !s.isBlank()) user += "\n教学风格偏好：" + s;
            if (input.get("extraRequirements") instanceof String s && !s.isBlank()) user += "\n额外要求：" + s;
        } else if (input.get("focus") instanceof String s && !s.isBlank()) user += "\n重点关注：" + s;
        var body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "system", "content", prompt), Map.of("role", "user", "content", user)));
        body.put("temperature", lesson ? 0.5 : 0.3);
        body.put("max_tokens", lesson ? 8192 : 4096);
        body.put("top_p", 1.0);
        body.put("stream", stream);
        if (!stream) body.put("response_format", Map.of("type", "json_object"));
        return body;
    }

    private HttpURLConnection connect(Map<String, Object> body) throws IOException {
        if (apiKey.isBlank()) throw new AIException(503, "未配置 DEEPSEEK_API_KEY", "auth");
        var conn = (HttpURLConnection) URI.create(baseUrl + "/chat/completions").toURL().openConnection();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Accept", Boolean.TRUE.equals(body.get("stream")) ? "text/event-stream" : "application/json");
        try {
            try (var out = conn.getOutputStream()) { out.write(mapper.writeValueAsBytes(body)); }
            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                // 不向浏览器透传上游错误正文，避免泄露供应商内部信息。
                throw new AIException(status >= 400 && status <= 599 ? status : 502,
                        "模型服务请求失败（HTTP " + status + "）",
                        status == 401 || status == 403 ? "auth" : status == 429 ? "rate_limit" : "server");
            }
            return conn;
        } catch (IOException | RuntimeException ex) { conn.disconnect(); throw ex; }
    }

    public JsonNode generate(Map<String, Object> input, boolean lesson) throws IOException {
        validate(input);
        var body = payload(input, lesson, false);
        for (int attempt = 0; ; attempt++) {
            try {
                var conn = connect(body);
                try (var in = conn.getInputStream()) {
                    JsonNode response;
                    try { response = mapper.readTree(in); }
                    catch (RuntimeException ex) { throw new AIException(502, "模型响应格式不正确", "response"); }
                    if (response == null) throw new AIException(502, "模型返回空响应", "response");
                    String text = response.path("choices").path(0).path("message").path("content").asText("");
                    int start = text.indexOf('{'), end = text.lastIndexOf('}');
                    if (start < 0 || end < start) throw new AIException(502, "模型输出无法解析为 JSON", "response");
                    try { return mapper.readTree(text.substring(start, end + 1)); }
                    catch (RuntimeException ex) { throw new AIException(502, "模型输出无法解析为 JSON", "response"); }
                } finally { conn.disconnect(); }
            } catch (AIException ex) {
                if (attempt >= 3 || !Set.of(429, 500, 502, 503, 504).contains(ex.status) || ex.type.equals("response") || apiKey.isBlank()) throw ex;
                try { Thread.sleep((1000L << attempt) + new Random().nextInt(1000)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("请求已取消", interrupted); }
            }
        }
    }

    public void stream(Map<String, Object> input, OutputStream output) throws IOException {
        validate(input);
        var conn = connect(payload(input, true, true));
        try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean done = false;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.equals("[DONE]")) { done = true; break; }
                if (data.isEmpty()) continue;
                JsonNode node;
                try { node = mapper.readTree(data); }
                catch (RuntimeException ex) { throw new AIException(502, "模型流式响应格式不正确", "response"); }
                if (node == null) throw new AIException(502, "模型返回空事件", "response");
                if (node.has("error")) throw new AIException(502, "模型流式生成失败", "server");
                String chunk = node.path("choices").path(0).path("delta").path("content").asText("");
                if (!chunk.isEmpty()) event(output, Map.of("type", "chunk", "content", chunk));
            }
            if (!done) throw new AIException(502, "模型流式响应意外中断，请重试", "network");
            event(output, Map.of("type", "done"));
        } finally { conn.disconnect(); }
    }

    public void event(OutputStream output, Map<String, Object> event) throws IOException {
        output.write(("data: " + mapper.writeValueAsString(event) + "\n\n").getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    public static class AIException extends RuntimeException {
        public final int status;
        public final String type;
        public AIException(int status, String message, String type) { super(message); this.status = status; this.type = type; }
    }
}
