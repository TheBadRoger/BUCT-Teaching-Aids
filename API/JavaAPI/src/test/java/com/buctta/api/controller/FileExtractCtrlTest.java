package com.buctta.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileExtractCtrlTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FileExtractCtrl()).build();
    }

    @Test
    void upload_noFiles_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/fileextract/temp"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_invalidExtension_returnsFailureItem() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "a.txt",
                "text/plain",
                "abc".getBytes()
        );

        mockMvc.perform(multipart("/api/fileextract/temp").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data[0].fileName").value("a.txt"))
                .andExpect(jsonPath("$.data[0].success").value(false));
    }

    @Test
    void upload_emptyFile_returnsEmptyResultList() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "empty.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/fileextract/temp").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
