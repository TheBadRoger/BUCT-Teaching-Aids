package com.buctta.api.service;

import tools.jackson.databind.JsonNode;

/**
 * 背诵手册生成服务。
 * <p>
 * 输入教材/笔记文本内容，调用 polymas 生成结构化背诵手册
 * （知识归纳、记忆口诀、必背要点、自测题）。
 */
public interface ExternalAIHandbookService {

    /**
     * 根据文件内容生成背诵手册。
     *
     * @param content   文件提取出的文本内容
     * @param topicName 可选的知识点名称（可为空，由 AI 自动识别）
     * @return 结构化的背诵手册 JSON（topic / content / metadata）
     */
    JsonNode generate(String content, String topicName);
}
