package com.buctta.api.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ExternalAIJudge {
    String submitTask(List<String> texts, List<String> fileNames);
    SseEmitter getEmitter(String id);
}
