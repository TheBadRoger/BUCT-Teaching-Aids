package com.buctta.api.serviceimp;

import com.buctta.api.service.ExternalAIHandbookService;
import com.buctta.api.utils.PolymasClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 背诵手册生成服务实现。
 * <p>
 * 复用 {@link PolymasClient} 调用外部 AI，把返回的 JSON 解析并规范化后交给前端渲染。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IMPL_ExternalAIHandbookService implements ExternalAIHandbookService {

    private final PolymasClient polymasClient;
    private final ObjectMapper objectMapper;

    /** 送入 AI 的文件内容最大字符数，超出截断，避免 token 超限 */
    private static final int MAX_CONTENT_LENGTH = 60000;

    /** 匹配 ```json ... ``` 代码块 */
    private static final Pattern CODE_BLOCK = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```");

    @Override
    public JsonNode generate(String content, String topicName) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("文件内容为空");
        }

        String trimmed = content.trim();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            trimmed = trimmed.substring(0, MAX_CONTENT_LENGTH);
            log.warn("文件内容过长，已截取前 {} 字符", MAX_CONTENT_LENGTH);
        }

        String question = buildPrompt(topicName, trimmed);

        String raw;
        try {
            raw = polymasClient.chat(question);
        } catch (Exception e) {
            throw new IllegalStateException("调用 AI 失败：" + e.getMessage(), e);
        }

        JsonNode parsed = parseAIJson(raw);
        return normalize(parsed, topicName);
    }

    /**
     * 组装发送给 polymas 的完整提示词（单条 question，无 system/user 分离）。
     */
    private String buildPrompt(String topicName, String content) {
        String topicContext = (topicName == null || topicName.isBlank())
                ? "请根据文件内容自动识别主要的知识点，自行确定知识归纳的标题。"
                : "用户已指定知识点为：「" + topicName + "」，请围绕此知识点组织内容。文件内容中与此知识点不直接相关的内容可适当略过。";

        return "你是一位经验丰富的中小学教师，擅长将教材内容转化为结构化的背诵材料。\n\n"
                + "用户上传了一份学习资料文件，请仔细阅读全部内容，提炼核心知识点并生成背诵手册。\n\n"
                + topicContext + "\n\n"
                + "请严格按照以下JSON格式输出（不要输出其他内容，只输出JSON）：\n\n"
                + "{\n"
                + "  \"topic\": \"知识点名称\",\n"
                + "  \"content\": {\n"
                + "    \"summary\": \"重点知识归纳（使用HTML标记，<h3>用于小节标题，<p>用于正文段落，<ul><li>用于列表，<strong>用于强调关键词）\",\n"
                + "    \"mnemonics\": \"记忆口诀（押韵的诗歌体，4-8句，每行用\\\\n换行符分隔）\",\n"
                + "    \"essentials\": [\n"
                + "      { \"type\": \"formula|definition|date|fact|rule|concept\", \"label\": \"要点名称（4-8字）\", \"value\": \"核心内容（简要，15字内）\", \"detail\": \"补充说明（可选，30字内）\" }\n"
                + "    ],\n"
                + "    \"selfTest\": [\n"
                + "      { \"id\": \"q-1\", \"question\": \"自测题题目\", \"answer\": \"正确答案\", \"hint\": \"解题思路或提示\", \"type\": \"choice|fill|short\" }\n"
                + "    ]\n"
                + "  },\n"
                + "  \"metadata\": {\n"
                + "    \"difficulty\": 3,\n"
                + "    \"tags\": [\"相关标签1\", \"相关标签2\", \"相关标签3\"],\n"
                + "    \"source\": \"用户上传学习资料\"\n"
                + "  }\n"
                + "}\n\n"
                + "要求：\n"
                + "1. summary 需结构清晰，分小节归纳，覆盖关键概念、核心内容、易错点、应用场景等\n"
                + "2. mnemonics 必须押韵，编成朗朗上口的歌诀或顺口溜，帮助学生记忆核心知识\n"
                + "3. essentials 列出4-6个最重要的必背知识点\n"
                + "4. selfTest 出4-6道自测题，题型混合（选择题、填空题、简答题），难度递进\n"
                + "5. 所有内容必须基于用户上传的文件，内容准确、适合学生学习使用\n"
                + "6. topic 名称要准确概括文件中的核心知识点\n\n"
                + "---文件内容---\n" + content + "\n---内容结束---";
    }

    /**
     * 把 AI 返回的原始文本解析为 JSON（兼容纯 JSON / 代码块 / 夹带文字三种情况）。
     */
    private JsonNode parseAIJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignore) {
            // 继续尝试其他方式
        }

        Matcher m = CODE_BLOCK.matcher(raw);
        if (m.find()) {
            try {
                return objectMapper.readTree(m.group(1));
            } catch (Exception ignore) {
                // 继续
            }
        }

        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                return objectMapper.readTree(raw.substring(start, end + 1));
            } catch (Exception ignore) {
                // 继续
            }
        }

        throw new IllegalStateException("AI 返回内容无法解析为 JSON，请重试");
    }

    /**
     * 规范化：补齐 id / subject / gradeLevel / topic 字段，方便前端直接渲染。
     */
    private JsonNode normalize(JsonNode parsed, String topicName) {
        ObjectNode result = objectMapper.createObjectNode();
        JsonNode content = parsed.path("content");
        JsonNode metadata = parsed.path("metadata");
        JsonNode tags = metadata.path("tags");

        String subject = "自定义";
        if (tags.isArray() && tags.size() > 0) {
            String first = tags.get(0).asText();
            if (first != null && !first.isBlank()) {
                subject = first;
            }
        }

        String topic = parsed.path("topic").asText();
        if (topic == null || topic.isBlank()) {
            topic = (topicName == null || topicName.isBlank()) ? "未命名知识点" : topicName;
        }

        result.put("id", "ai-" + System.currentTimeMillis());
        result.put("subject", subject);
        result.put("gradeLevel", "自定义");
        result.put("topic", topic);
        result.set("content", content);
        result.set("metadata", metadata);
        return result;
    }
}
