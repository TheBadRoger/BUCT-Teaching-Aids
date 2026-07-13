package com.buctta.api.controller;

import com.buctta.api.entities.Course;
import com.buctta.api.service.CourseViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CoursePopularityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseViewService courseViewService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private CoursePopularityController coursePopularityController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(coursePopularityController).build();
    }

    @Test
    void getRanking_invalidLimit_returns4003() throws Exception {
        mockMvc.perform(get("/api/course/popularity/ranking").param("limit", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void getRanking_success_returnsStatsAndItems() throws Exception {
        Course c1 = new Course();
        c1.setId(1L);
        c1.setCourseName("Java");
        c1.setViewCount(100L);
        c1.setCourseStatus("OPEN");

        Course c2 = new Course();
        c2.setId(2L);
        c2.setCourseName("Spring");
        c2.setViewCount(50L);
        c2.setCourseStatus("OPEN");

        when(courseViewService.getPopularCourses(2)).thenReturn(List.of(c1, c2));
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size("popular:courses")).thenReturn(2L);

        mockMvc.perform(get("/api/course/popularity/ranking").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.items[0].courseName").value("Java"))
                .andExpect(jsonPath("$.data.stats.totalViews").value(150))
                .andExpect(jsonPath("$.data.stats.averageViews").value(75));
    }

    @Test
    void isPopular_courseInTopList_returnsTrue() throws Exception {
        Course c1 = new Course();
        c1.setId(3L);
        when(courseViewService.getPopularCourses(10)).thenReturn(List.of(c1));

        mockMvc.perform(get("/api/course/popularity/3/is-popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getCourseRanking_notFound_returnsZero() throws Exception {
        Course c1 = new Course();
        c1.setId(3L);
        when(courseViewService.getPopularCourses(Integer.MAX_VALUE)).thenReturn(List.of(c1));

        mockMvc.perform(get("/api/course/popularity/99/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").value(0));
    }
}

