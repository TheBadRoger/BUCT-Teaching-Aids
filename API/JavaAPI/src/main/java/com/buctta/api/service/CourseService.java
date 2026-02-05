package com.buctta.api.service;

import com.buctta.api.entities.CourseList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {
    CourseList addCourse(CourseList courseList);
    Page<CourseList> searchCourses(String courseName, String courseNumber,
                                   String teachingTeachers, String courseStatus,
                                   String courseTags, String startDate, Pageable pageable);
    CourseList updateCourse(Long id, CourseList courseDetails);
    CourseList getCourseById(Long id);
    boolean deleteCourse(Long id);
}