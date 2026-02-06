package com.buctta.api.serviceimp;

import com.buctta.api.service.ExternalAIJudge;
import com.buctta.api.utils.ExternalAI;
import com.buctta.api.utils.SSEResponseContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMPL_ExternalAIJudge implements ExternalAIJudge {

    private final ExternalAI aiProps = new ExternalAI(
            "https://cloudapi.polymas.com/bot/v2/completions/chat/stream",
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJsb2dpbiIsImxvZ2luSWQiOiJON0k3cFNUcm0zIiwicm5TdHIiOiJkRllocHFhWkdkQjczY3dkM215eTNqN29XSm9HdTdyYSIsInR5cGUiOiJaSFMiLCJ1c2VyTmlkIjoiTjdJN3BTVHJtMyJ9.o99-sfD3_TmewtrmT7O-ItrLjGKAIMSaEdPGFMaoU0U",
            "(AI生成)");

    private final ObjectMapper objectMapper; // 由 Spring 注入单例
    private final ThreadPoolTaskExecutor aiExecutor;
    /* 线程安全 Map + 弱引用，防止内存泄漏 */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    public String submitTask(List<String> texts, List<String> fileNames) {
        String id = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        emitterMap.put(id, emitter);
        /* 使用 Spring 托管的线程池 */
        aiExecutor.submit(() -> doGenerate(id, emitter, texts, fileNames));
        return id;
    }

    public SseEmitter getEmitter(String id) {
        return emitterMap.computeIfAbsent(id, k -> {
            SseEmitter tmp = new SseEmitter(5_000L);
            try {
                tmp.send(SseEmitter.event().name("fail").data("no such id: " + k));
            } catch (IOException ignore) {
            }
            tmp.complete();
            return tmp;
        });
    }

    /* ---------- 真正生成逻辑 ---------- */
    private void doGenerate(String id, SseEmitter emitter,
                            List<String> texts, List<String> fileNames) {
        int total = texts.size();
        try {
            for (int index = 0; index < total; index++) {
                send(emitter, "fileStart", Map.of("index", index, "total", total));
                String result = callAi(index, texts.get(index), fileNames.get(index));
                send(emitter, "message", result);
            }
            send(emitter, "done", "[COMPLETED]");
            emitter.complete();
        } catch (Exception e) {
            log.error("AI generate fail", e);
            send(emitter, "fail", "生成异常：" + e.getMessage());
            emitter.completeWithError(e);
        } finally {
            emitterMap.remove(id);
        }
    }

    /* ---------- 调用外部 SSE ---------- */
    private String callAi(int id, String text, String fileName) throws IOException {
        HttpURLConnection conn = buildConnection();
        String payload = buildPayload(text);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        int st = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        (st >= 200 && st < 400) ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {

            StringBuilder buf = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("event:OUT_COMPLETE")) continue;
                /* 读 data 行 */
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String seg = line.substring(5).trim()
                                .replaceAll("^\\{\"text\":\"", "")
                                .replaceAll("\"}$", "");
                        if (seg.contains(aiProps.getStopMark())) {
                            buf.append(seg, 0, seg.indexOf(aiProps.getStopMark()));
                            return parseAndFormat(id, buf.toString(), fileName);
                        }
                    }
                }
            }
            return "AI 未返回有效内容\n";
        }
    }

    /* ---------- 解析 AI 返回 & 拼装 ---------- */

    private String parseAndFormat(int id, String raw, String fileName) throws IOException {
        JsonNode node;

        raw = raw.replace("\\\"", "\"");
        node = objectMapper.readTree(raw);

        String stem = fileName.replaceFirst("\\.[^.]+$", "");
        String[] seg = stem.split("_", -1);   // -1 保证空串也保留
        if (seg.length != 5) {
            throw new IllegalArgumentException("文件名格式错误：" + fileName);
        }

        int score = node.get("分数").asInt();
        String basis = node.get("评分依据").asString();

        return String.format(
                "姓名：%s\n学号：%s\n班级：%s\n日期：%s\n报告名称：%s\n\n分数：%d\n评判依据：%s\n\n",
                seg[0], seg[3], seg[4], seg[1], seg[2], score, basis);
    }

    /* ---------- 工具 ---------- */
    private HttpURLConnection buildConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(aiProps.getEndPoint()).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(0);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("authorization", aiProps.getAuthKey());
        return conn;
    }

    private String buildPayload(String question) {
        // 用 ObjectNode 一次性生成，库会自动加引号、转义
        ObjectNode node = objectMapper.createObjectNode();
        node.put("appCode", "ti39Ohdy6k");
        node.put("userNid", "N7I7pSTrm3");
        node.put("sessionNid", "PNeg1BjP6x");
        node.put("chatNid", "JJdylJaMSF");
        node.put("testFlag", true);
        node.put("reasoningFlag", false);
        node.putObject("metadata").put("thinkingEnabled", 0);
        node.put("question", question);   // 原始字符串，不要 toJsonString
        return node.toString();
    }

    private void send(SseEmitter emitter, String type, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(type)
                    .data(new SSEResponseContainer<>(type, data)));
        } catch (IOException e) {
            log.warn("SSE send fail", e);
        }
    }
}