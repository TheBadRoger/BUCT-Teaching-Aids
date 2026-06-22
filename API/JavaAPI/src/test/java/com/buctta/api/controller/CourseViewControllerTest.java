package com.buctta.api.controller;

import com.buctta.api.entities.Course;
import com.buctta.api.service.CourseViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CourseViewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseViewService courseViewService;

    @InjectMocks
    private CourseViewController courseViewController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseViewController).build();
    }

    @Test
    void getPopularCourses_invalidLimit_returns4003() throws Exception {
        mockMvc.perform(get("/api/course/view/popular").param("limit", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void getPopularCourses_success_returns200() throws Exception {
        Course course = new Course();
        course.setId(1L);
        course.setCourseName("Java");

        when(courseViewService.getPopularCourses(1)).thenReturn(List.of(course));

        mockMvc.perform(get("/api/course/view/popular").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data[0].courseName").value("Java"));
    }

    @Test
    void getCourseViewCount_success_returnsCount() throws Exception {
        when(courseViewService.getCourseViewCount(1L)).thenReturn(99L);

        mockMvc.perform(get("/api/course/view/1/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").value(99));
    }

    @Test
    void recordCourseView_success_returnsMessage() throws Exception {
        mockMvc.perform(post("/api/course/view/2/record"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));

        verify(courseViewService).recordCourseView(2L);
    }

    @Test
    void clearCourseViewCount_invalidCourseId_returns4003() throws Exception {
        mockMvc.perform(delete("/api/course/view/admin/clear/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }
}

