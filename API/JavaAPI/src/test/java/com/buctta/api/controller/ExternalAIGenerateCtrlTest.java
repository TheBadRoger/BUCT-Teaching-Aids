package com.buctta.api.controller;

import com.buctta.api.service.ExternalAIJudgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExternalAIGenerateCtrlTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ExternalAIJudgeService aiGenerateService;

    @InjectMocks
    private ExternalAIGenerateCtrl externalAIGenerateCtrl;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(externalAIGenerateCtrl).build();
    }

    @Test
    void startJudge_countsIsOne_mergesExtractedTextsThenSubmits() throws Exception {
        Map<String, Object> request = Map.of(
                "extractedTexts", List.of("A", "B", "C"),
                "fileNames", List.of("report.txt"),
                "counts", 1
        );

        when(aiGenerateService.submitTask(List.of("A", "B", "C"), List.of("report.txt"))).thenReturn("task-1");

        mockMvc.perform(post("/api/ai/generate/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").value("task-1"))
                .andExpect(jsonPath("$.data.status").value("started"));

        verify(aiGenerateService).submitTask(List.of("A", "B", "C"), List.of("report.txt"));
    }

    @Test
    void startJudge_countsNotOne_submitsOriginalLists() throws Exception {
        Map<String, Object> request = Map.of(
                "extractedTexts", List.of("X", "Y"),
                "fileNames", List.of("f1.txt", "f2.txt"),
                "counts", 2
        );

        when(aiGenerateService.submitTask(List.of("X", "Y"), List.of("f1.txt", "f2.txt"))).thenReturn("task-2");

        mockMvc.perform(post("/api/ai/generate/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").value("task-2"))
                .andExpect(jsonPath("$.data.status").value("started"));

        verify(aiGenerateService).submitTask(List.of("X", "Y"), List.of("f1.txt", "f2.txt"));
    }

    @Test
    void startJudge_countsIsOne_withEmptyExtractedTexts_returnsStarted() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("extractedTexts", List.of());
        request.put("fileNames", List.of("report.txt"));
        request.put("counts", 1);

        when(aiGenerateService.submitTask(List.of(), List.of("report.txt"))).thenReturn("task-empty");

        mockMvc.perform(post("/api/ai/generate/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.id").value("task-empty"))
                .andExpect(jsonPath("$.data.status").value("started"));

        verify(aiGenerateService).submitTask(eq(List.of()), eq(List.of("report.txt")));
    }

    @Test
    void stream_validId_returnsEmitter() throws Exception {
        when(aiGenerateService.getEmitter("task-1")).thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/ai/generate/stream/task-1"))
                .andExpect(status().isOk());

        verify(aiGenerateService).getEmitter("task-1");
    }
}
