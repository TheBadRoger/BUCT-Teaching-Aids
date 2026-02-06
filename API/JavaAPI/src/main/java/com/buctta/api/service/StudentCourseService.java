package com.buctta.api.service;

import com.buctta.api.entities.StudentCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentCourseService {
    StudentCourse selectCourse(Long studentId, Long courseId);

    StudentCourse updateViewedStatus(Long studentId, Long courseId, Boolean isViewed);

    Page<StudentCourse> getAllCourses(Long studentId, Pageable pageable);

    Page<StudentCourse> getViewedCourses(Long studentId, Pageable pageable);

    Page<StudentCourse> getNotViewedCourses(Long studentId, Pageable pageable);

    void dropCourse(Long studentId, Long courseId);

    Page<StudentCourse> searchStudentCourses(String studentName, String courseName,
                                             Boolean isViewed, Long studentId, Pageable pageable);
}
