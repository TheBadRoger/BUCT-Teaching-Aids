package com.buctta.api.controller;

import com.buctta.api.entities.Teacher;
import com.buctta.api.service.TeacherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherListCtrlTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TeacherService teacherService;

    @InjectMocks
    private TeacherListCtrl teacherListCtrl;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(teacherListCtrl).build();
    }

    @Test
    void addTeacher_success_returns200() throws Exception {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setName("Prof. Li");

        when(teacherService.addTeacher(any(Teacher.class))).thenReturn(TeacherService.TeacherResult.success(teacher));

        mockMvc.perform(post("/api/teacher/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.name").value("Prof. Li"));
    }

    @Test
    void addTeacher_entityExists_returns4091() throws Exception {
        Teacher teacher = new Teacher();
        teacher.setName("Prof. Li");

        when(teacherService.addTeacher(any(Teacher.class)))
                .thenReturn(TeacherService.TeacherResult.fail("TEACHER_EXISTS", "exists"));

        mockMvc.perform(post("/api/teacher/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4091));
    }

    @Test
    void searchTeacher_success_returnsPage() throws Exception {
        Teacher teacher = new Teacher();
        teacher.setId(2L);
        teacher.setName("Wang");
        Page<Teacher> page = new PageImpl<>(List.of(teacher), PageRequest.of(0, 10), 1);

        when(teacherService.searchTeachers(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(post("/api/teacher/search")
                        .param("name", "Wang")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.content[0].name").value("Wang"));
    }

    @Test
    void searchTeacher_serviceThrows_returns5000() throws Exception {
        when(teacherService.searchTeachers(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("db error"));

        mockMvc.perform(post("/api/teacher/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5000));
    }
}

