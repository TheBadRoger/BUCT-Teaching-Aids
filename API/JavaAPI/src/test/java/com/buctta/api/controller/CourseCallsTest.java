package com.buctta.api.controller;

import com.buctta.api.entities.Course;
import com.buctta.api.service.CourseService;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CourseCallsTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseCalls courseCalls;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseCalls)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // ─── /add ────────────────────────────────────────────────────────────────

    @Test
    void addCourse_success_returns200WithCourse() throws Exception {
        Course course = new Course();
        course.setId(1L);
        course.setCourseName("Java Programming");
        course.setCourseNumber("CS101");
        course.setViewCount(0L);

        when(courseService.addCourse(any(Course.class)))
                .thenReturn(CourseService.CourseResult.success(course));

        mockMvc.perform(post("/api/course/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(course)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.courseNumber").value("CS101"));
    }

    @Test
    void addCourse_existingCourseNumber_returns4091() throws Exception {
        Course course = new Course();
        course.setCourseNumber("CS101");
        course.setViewCount(0L);

        when(courseService.addCourse(any(Course.class)))
                .thenReturn(CourseService.CourseResult.fail("COURSE_NUMBER_EXISTS", "课程编号已存在"));

        mockMvc.perform(post("/api/course/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(course)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4091));
    }

    // ─── /search ─────────────────────────────────────────────────────────────

    @Test
    void searchCourses_noFilters_returnsPage() throws Exception {
        Course course = new Course();
        course.setId(1L);
        course.setCourseName("Java Programming");
        course.setViewCount(0L);
        Page<Course> page = new PageImpl<>(List.of(course), PageRequest.of(0, 10), 1);

        when(courseService.searchCourses(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/course/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.content[0].courseName").value("Java Programming"));
    }

    @Test
    void searchCourses_withCourseNameFilter_returnsFilteredPage() throws Exception {
        Course course = new Course();
        course.setId(1L);
        course.setCourseName("Java Programming");
        course.setViewCount(0L);
        Page<Course> page = new PageImpl<>(List.of(course), PageRequest.of(0, 10), 1);

        when(courseService.searchCourses(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/course/search")
                        .param("courseName", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
