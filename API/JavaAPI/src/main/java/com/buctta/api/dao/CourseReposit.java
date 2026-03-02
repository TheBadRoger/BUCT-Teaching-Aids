package com.buctta.api.dao;

import com.buctta.api.entities.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CourseReposit extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    //Course findCourseById(long id);
    Course findCourseByCourseNumber(String courseNumber);

    //Course findCourseListByCourseName(String courseName);
    Optional<Course> findCourseById(Long id);
}

