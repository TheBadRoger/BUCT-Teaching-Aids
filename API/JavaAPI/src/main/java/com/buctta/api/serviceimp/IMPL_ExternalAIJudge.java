package com.buctta.api.serviceimp;

import com.buctta.api.service.ExternalAIJudge;
import com.buctta.api.utils.ExternalAI;
import com.buctta.api.utils.JsonUtil;
import com.buctta.api.utils.SSEResponseContainer;
import com.buctta.api.utils.ThreadPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMPL_ExternalAIJudge implements ExternalAIJudge {
    private final ExternalAI aiProps =ExternalAI.AIJudgement();
    private final ObjectMapper objectMapper; // 由 Spring 注入单例

    /* 线程安全 Map + 弱引用，防止内存泄漏 */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    public String submitTask(List<String> texts, List<String> fileNames) {
        String id = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        emitterMap.put(id, emitter);

        /* 使用 Spring 托管的线程池（自动关闭）*/
        ThreadPoolTaskExecutor executor = ThreadPool.create(
                8, 32, 200, "ai-gen-");
        executor.submit(() -> doGenerate(id, emitter, texts, fileNames));
        return id;
    }

    public SseEmitter getEmitter(String id) {
        return emitterMap.computeIfAbsent(id, k -> {
            SseEmitter tmp = new SseEmitter(5_000L);
            try {
                tmp.send(SseEmitter.event().name("error").data("no such id: " + k));
            } catch (IOException ignore) {}
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

                String result = callAi(texts.get(index), fileNames.get(index));
                send(emitter, "message", result);

                send(emitter, "fileEnd", Map.of("index", index, "total", total));
            }
            send(emitter, "done", "[COMPLETED]");
            emitter.complete();
        } catch (Exception e) {
            log.error("AI generate error", e);
            send(emitter, "error", "生成异常：" + e.getMessage());
            emitter.completeWithError(e);
        } finally {
            emitterMap.remove(id);
        }
    }

    /* ---------- 调用外部 SSE ---------- */
    private String callAi(String text, String fileName) throws IOException {
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
                            return parseAndFormat(buf.toString(), fileName);
                        }
                    }
                }
            }
            return "AI 未返回有效内容";
        }
    }

    /* ---------- 解析 AI 返回 & 拼装 ---------- */
    private String parseAndFormat(String raw, String fileName) throws IOException {
        JsonNode node = objectMapper.readValue(raw, JsonNode.class);

        String name = "Unidentified", date = "Unidentified",
                exp = "Unidentified", id = "Unidentified";
        Matcher m = Pattern.compile("([^_]+)_(\\d{8})_(.+)_([^.]+)\\.\\w+")
                .matcher(fileName);
        if (m.matches()) {
            name = m.group(1);
            date = m.group(2);
            exp  = m.group(3);
            id   = m.group(4);
        }
        int    score  = node.get("分数").asInt();
        String basis  = node.get("评分依据").asText();

        return String.format(
                "姓名：%s\n学号：%s\n日期：%s\n报告名称：%s\n分数：%d\n评判依据：\n%s",
                name, id, date, exp, score, basis);
    }

    /* ---------- 工具 ---------- */
    private HttpURLConnection buildConnection() throws IOException {
        HttpURLConnection conn =
                (HttpURLConnection) new URL(aiProps.getEndPoint()).openConnection();
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
        return String.format(
                "{\"appCode\":\"ti39Ohdy6k\",\"userNid\":\"N7I7pSTrm3\"," +
                        "\"sessionNid\":\"PNeg1BjP6x\",\"chatNid\":\"JJdylJaMSF\"," +
                        "\"testFlag\":true,\"reasoningFlag\":false," +
                        "\"metadata\":{\"thinkingEnabled\":0},\"question\":%s}",
                JsonUtil.toJsonString(question));
    }

    private void send(SseEmitter emitter, String type, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(type)
                    .data(new SSEResponseContainer<>(type, data)));
        } catch (IOException e) {
            log.warn("SSE send error", e);
        }
    }
}