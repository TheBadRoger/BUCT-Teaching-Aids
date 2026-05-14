package com.buctta.api.dao;

import com.buctta.api.entities.Course;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseReposit extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    //Course findCourseById(long id);
    Course findCourseByCourseNumber(String courseNumber);

    //Course findCourseListByCourseName(String courseName);
    Optional<Course> findCourseById(Long id);
    @Modifying
    @Transactional
    @Query("DELETE FROM Course c WHERE c.id IN :ids")
    void deleteAllByIdIn(@Param("ids") List<Long> ids);

}

