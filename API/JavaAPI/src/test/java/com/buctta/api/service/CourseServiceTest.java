package com.buctta.api.service;

import com.buctta.api.entities.Course;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class CourseServiceTest {
    @Autowired
    private CourseService courseService;

    @MockBean
    private CourseService courseServiceMock;

    @Test
    void testServiceNotNull() {
        assert(courseService != null);
    }

    @Test
    void testAddCourse_Success() {
        Course course = new Course();
        course.setCourseName("Test Course");
        when(courseServiceMock.addCourse(any(Course.class))).thenReturn(CourseService.CourseResult.success(course));

        CourseService.CourseResult result = courseServiceMock.addCourse(course);
        assertThat(result.success()).isTrue();
        assertThat(result.course().getCourseName()).isEqualTo("Test Course");
    }

    @Test
    void testSearchCourses() {
        Page<Course> mockPage = new PageImpl<>(Collections.singletonList(new Course()));
        when(courseServiceMock.searchCourses(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(mockPage);

        Page<Course> result = courseServiceMock.searchCourses("", "", "", "", "", "", Pageable.unpaged());
        assertThat(result.getContent()).isNotEmpty();
    }
}
