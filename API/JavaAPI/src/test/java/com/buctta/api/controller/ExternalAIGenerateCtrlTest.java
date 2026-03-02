package com.buctta.api.controller;

import com.buctta.api.service.ExternalAIJudgeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ExternalAIGenerateCtrlTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private ExternalAIJudgeService aiJudgeService;

    @SuppressWarnings("unchecked")
    @Test
    void testStartJudge_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        when(aiJudgeService.submitTask(any(List.class), any(List.class))).thenReturn("12345");

        mockMvc.perform(post("/api/ai/generate/start")
                        .param("extractedTexts", "Sample text")
                        .param("fileNames", "file1.docx")
                        .param("counts", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void testStream_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        when(aiJudgeService.getEmitter("12345")).thenReturn(Mockito.mock(SseEmitter.class));

        mockMvc.perform(get("/api/ai/generate/stream/12345"))
                .andExpect(status().isOk());
    }
}
