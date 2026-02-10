package com.buctta.api.service;

import com.buctta.api.entities.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {
    Course addCourse(Course course);

    Page<Course> searchCourses(String courseName, String courseNumber,
                               String teachingTeachers, String courseStatus,
                               String courseTags, String startDate, Pageable pageable);

    Course updateCourse(Long id, Course courseDetails);

    Course getCourseById(Long id);

    boolean deleteCourse(Long id);
}