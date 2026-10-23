package com.buctta.api.controller;

import com.buctta.api.entities.StudentCourse;
import com.buctta.api.service.StudentCourseService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentCourseCtrlTest {

    private MockMvc mockMvc;

    @Mock
    private StudentCourseService studentCourseService;

    @InjectMocks
    private StudentCourseCtrl studentCourseCtrl;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studentCourseCtrl).build();
    }

    @Test
    void selectCourse_success_returns200() throws Exception {
        StudentCourse studentCourse = new StudentCourse();
        when(studentCourseService.selectCourse(1L, 2L))
                .thenReturn(StudentCourseService.CourseOperationResult.success(studentCourse));

        mockMvc.perform(post("/api/student-courses/select")
                        .param("studentId", "1")
                        .param("courseId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void selectCourse_failure_returns5000() throws Exception {
        when(studentCourseService.selectCourse(1L, 2L))
                .thenReturn(StudentCourseService.CourseOperationResult.fail("SELECT_FAILED", "failed"));

        mockMvc.perform(post("/api/student-courses/select")
                        .param("studentId", "1")
                        .param("courseId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5000));
    }

    @Test
    void updateViewedStatus_success_returns200() throws Exception {
        StudentCourse studentCourse = new StudentCourse();
        when(studentCourseService.updateViewedStatus(1L, 2L, true))
                .thenReturn(StudentCourseService.CourseOperationResult.success(studentCourse));

        mockMvc.perform(put("/api/student-courses/update-viewed")
                        .param("studentId", "1")
                        .param("courseId", "2")
                        .param("isViewed", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void getAllCourses_success_returnsPage() throws Exception {
        Page<StudentCourse> page = new PageImpl<>(List.of(new StudentCourse()), PageRequest.of(0, 10), 1);
        when(studentCourseService.getAllCourses(any(Long.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/student-courses/all-courses")
                        .param("studentId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void dropCourse_failure_returns5000() throws Exception {
        when(studentCourseService.dropCourse(1L, 2L))
                .thenReturn(StudentCourseService.CourseOperationResult.fail("DROP_FAILED", "failed"));

        mockMvc.perform(delete("/api/student-courses/drop")
                        .param("studentId", "1")
                        .param("courseId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5000));
    }
}

