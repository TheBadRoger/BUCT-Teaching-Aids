package com.buctta.api.utils;

import com.buctta.api.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * polymas 第三方 AI 平台的通用客户端。
 * <p>
 * 把原先散落在「AI 批改服务」里的 polymas 调用逻辑抽出来，
 * 供批改、背诵手册等所有需要调用该 AI 的场景复用。
 * 密钥从配置（AppProperties）读取，不再硬编码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolymasClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    /**
     * 向 polymas 发送一个问题，返回累积的完整回答文本。
     *
     * @param question 发送给 AI 的问题/提示词
     * @return 去掉结束标记（stopMark）之后的回答正文
     */
    public String chat(String question) throws IOException {
        AppProperties.Polymas cfg = appProperties.getAi().getPolymas();

        if (cfg.getAuthKey() == null || cfg.getAuthKey().isBlank()) {
            throw new IOException("未配置 polymas 密钥，请在 .env 里设置 POLYMAS_AUTH_KEY");
        }

        HttpURLConnection conn = buildConnection(cfg);
        String payload = buildPayload(cfg, question);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (status >= 200 && status < 400) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {

            StringBuilder buf = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("event:OUT_COMPLETE")) {
                    continue;
                }
                // 读 data 行，逐段累积
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String seg = line.substring(5).trim()
                                .replaceAll("^\\{\"text\":\"", "")
                                .replaceAll("\"}$", "");
                        if (seg.contains(cfg.getStopMark())) {
                            buf.append(seg, 0, seg.indexOf(cfg.getStopMark()));
                            return buf.toString();
                        }
                        buf.append(seg);
                    }
                }
            }
            return buf.length() > 0 ? buf.toString() : "AI 未返回有效内容";
        }
    }

    private HttpURLConnection buildConnection(AppProperties.Polymas cfg) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(cfg.getEndpoint()).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(0);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("authorization", cfg.getAuthKey());
        return conn;
    }

    private String buildPayload(AppProperties.Polymas cfg, String question) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("appCode", cfg.getAppCode());
        node.put("userNid", cfg.getUserNid());
        node.put("sessionNid", cfg.getSessionNid());
        node.put("chatNid", cfg.getChatNid());
        node.put("testFlag", true);
        node.put("reasoningFlag", false);
        node.putObject("metadata").put("thinkingEnabled", 0);
        node.put("question", question);
        return node.toString();
    }
}
