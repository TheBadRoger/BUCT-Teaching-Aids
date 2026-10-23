package com.buctta.api.serviceimp;

import com.buctta.api.service.ExternalAIJudgeService;
import com.buctta.api.utils.PolymasClient;
import com.buctta.api.utils.SSEResponseContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMPL_ExternalAIJudgeService implements ExternalAIJudgeService {

    private final PolymasClient polymasClient;
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
            }
            catch (IOException ignore) {
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
                String raw = polymasClient.chat(texts.get(index));
                String result = parseAndFormat(raw, fileNames.get(index));
                send(emitter, "message", result);
            }
            send(emitter, "done", "[COMPLETED]");
            emitter.complete();
        }
        catch (Exception e) {
            log.error("AI generate fail", e);
            send(emitter, "fail", "生成异常：" + e.getMessage());
            emitter.completeWithError(e);
        }
        finally {
            emitterMap.remove(id);
        }
    }

    /* ---------- 解析 AI 返回 & 拼装 ---------- */

    private String parseAndFormat(String raw, String fileName) {
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

    private void send(SseEmitter emitter, String type, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(type)
                    .data(new SSEResponseContainer<>(type, data)));
        }
        catch (IOException e) {
            log.warn("SSE send fail", e);
        }
    }
}
