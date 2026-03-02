package com.buctta.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class FileExtractCtrlTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void testUpload_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Test content".getBytes()
        );

        mockMvc.perform(multipart("/api/fileextract/temp").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void testUpload_EmptyFile() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        MockMultipartFile file = new MockMultipartFile(
                "files", "", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[0]
        );

        mockMvc.perform(multipart("/api/fileextract/temp").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpload_InvalidFormat() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.txt", "text/plain", "Invalid content".getBytes()
        );

        mockMvc.perform(multipart("/api/fileextract/temp").file(file))
                .andExpect(status().isBadRequest());
    }
}
