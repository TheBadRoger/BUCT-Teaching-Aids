package com.buctta.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileDownloadCtrlTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FileDownloadCtrl()).build();
    }

    @Test
    void downloadReport_validText_returnsExcelAttachment() throws Exception {
        String text = "========== 第 1/1 个文件 ==========\n"
                + "姓名：张三\n"
                + "学号：2024001\n"
                + "班级：软件1班\n"
                + "日期：2026-04-05\n"
                + "报告名称：实验一\n"
                + "分数：90\n"
                + "评判依据：完成较好\n";

        mockMvc.perform(get("/api/generate/download/123456").param("text", text))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("judgereport_123456.xlsx")));
    }

    @Test
    void downloadReport_judgereportRouteByGet_returnsExcelAttachment() throws Exception {
        String text = "========== 第 1/1 个文件 ==========\n姓名：李四\n";

        mockMvc.perform(get("/api/generate/judgereport/1001").param("text", text))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("judgereport_1001.xlsx")));
    }

    @Test
    void downloadReport_judgereportRouteByPost_returnsExcelAttachment() throws Exception {
        String text = "========== 第 1/1 个文件 ==========\n姓名：王五\n";

        mockMvc.perform(post("/api/generate/judgereport/2002")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(text))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("judgereport_2002.xlsx")));
    }

    @Test
    void downloadReport_emptyTextByGet_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/generate/judgereport/3003").param("text", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("text parameter is required"));
    }

    @Test
    void downloadReport_blankTextByPost_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/generate/judgereport/4004")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("   "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("text parameter is required"));
    }
}

