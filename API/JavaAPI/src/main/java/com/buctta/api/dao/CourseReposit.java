package com.buctta.api.dao;

import com.buctta.api.entities.CourseList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CourseReposit extends JpaRepository<CourseList, Long>, JpaSpecificationExecutor<CourseList> {
    //CourseList findCourseListById(long id);
    CourseList findCourseListByCourseNumber(String courseNumber);

    //CourseList findCourseListByCourseName(String courseName);
    Optional<CourseList> findById(Long id);
}