package com.buctta.api.serviceimp;

import com.buctta.api.dao.CourseReposit;
import com.buctta.api.entities.Course;
import com.buctta.api.service.CourseService;
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
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IMPL_CourseServiceTest {

    @Mock
    private CourseReposit courseReposit;

    @InjectMocks
    private IMPL_CourseService courseService;

    private Course javaCourse;

    @BeforeEach
    void setUp() {
        javaCourse = new Course();
        javaCourse.setId(1L);
        javaCourse.setCourseName("Java Programming");
        javaCourse.setCourseNumber("CS101");
        javaCourse.setCourseStatus("active");
        javaCourse.setViewCount(0L);
    }

    // ─── addCourse ───────────────────────────────────────────────────────────

    @Test
    void addCourse_newCourseNumber_returnsSuccess() {
        when(courseReposit.findCourseByCourseNumber("CS101")).thenReturn(null);
        when(courseReposit.save(javaCourse)).thenReturn(javaCourse);

        CourseService.CourseResult result = courseService.addCourse(javaCourse);

        assertThat(result.success()).isTrue();
        assertThat(result.course()).isEqualTo(javaCourse);
    }

    @Test
    void addCourse_existingCourseNumber_returnsFail() {
        when(courseReposit.findCourseByCourseNumber("CS101")).thenReturn(javaCourse);

        CourseService.CourseResult result = courseService.addCourse(javaCourse);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("COURSE_NUMBER_EXISTS");
        verify(courseReposit, never()).save(any());
    }

    // ─── updateCourse ─────────────────────────────────────────────────────────

    @Test
    void updateCourse_courseExists_updatesAndReturnsSuccess() {
        when(courseReposit.findById(1L)).thenReturn(Optional.of(javaCourse));
        when(courseReposit.findCourseByCourseNumber("CS101")).thenReturn(javaCourse);
        when(courseReposit.save(javaCourse)).thenReturn(javaCourse);

        Course updates = new Course();
        updates.setCourseName("Advanced Java");
        updates.setCourseNumber("CS101");

        CourseService.CourseResult result = courseService.updateCourse(1L, updates);

        assertThat(result.success()).isTrue();
        assertThat(result.course().getCourseName()).isEqualTo("Advanced Java");
    }

    @Test
    void updateCourse_courseNotFound_returnsFail() {
        when(courseReposit.findById(99L)).thenReturn(Optional.empty());

        CourseService.CourseResult result = courseService.updateCourse(99L, new Course());

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("COURSE_NOT_FOUND");
    }

    @Test
    void updateCourse_duplicateCourseNumber_returnsFail() {
        Course otherCourse = new Course();
        otherCourse.setId(2L);
        otherCourse.setCourseNumber("CS102");

        when(courseReposit.findById(1L)).thenReturn(Optional.of(javaCourse));
        when(courseReposit.findCourseByCourseNumber("CS102")).thenReturn(otherCourse);

        Course updates = new Course();
        updates.setCourseNumber("CS102");

        CourseService.CourseResult result = courseService.updateCourse(1L, updates);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("COURSE_NUMBER_EXISTS");
    }

    // ─── getCourseById ────────────────────────────────────────────────────────

    @Test
    void getCourseById_found_returnsCourse() {
        when(courseReposit.findById(1L)).thenReturn(Optional.of(javaCourse));

        Course result = courseService.getCourseById(1L);

        assertThat(result).isEqualTo(javaCourse);
    }

    @Test
    void getCourseById_notFound_throwsException() {
        when(courseReposit.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseById(99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── deleteCourse ─────────────────────────────────────────────────────────

    @Test
    void deleteCourse_exists_returnsSuccess() {
        when(courseReposit.existsById(1L)).thenReturn(true);

        CourseService.CourseResult result = courseService.deleteCourse(1L);

        assertThat(result.success()).isTrue();
        verify(courseReposit).deleteById(1L);
    }

    @Test
    void deleteCourse_notFound_returnsFail() {
        when(courseReposit.existsById(99L)).thenReturn(false);

        CourseService.CourseResult result = courseService.deleteCourse(99L);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("COURSE_NOT_FOUND");
        verify(courseReposit, never()).deleteById(any());
    }

    // ─── searchCourses ────────────────────────────────────────────────────────

    @Test
    void searchCourses_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Course> page = new PageImpl<>(List.of(javaCourse), pageable, 1);
        when(courseReposit.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<Course> result = courseService.searchCourses(
                "Java", null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(javaCourse);
    }
}
