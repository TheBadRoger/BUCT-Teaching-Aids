package com.buctta.api.dao;

import com.buctta.api.entities.StudentCourse;
import com.buctta.api.entities.StudentCourseId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCourseReposit extends JpaRepository<StudentCourse, StudentCourseId>,
        JpaSpecificationExecutor<StudentCourse> {

    @Query("SELECT sc FROM StudentCourse sc WHERE sc.student.id = :studentId")
    List<StudentCourse> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT sc FROM StudentCourse sc WHERE sc.student.id = :studentId")
    Page<StudentCourse> findByStudentId(@Param("studentId") Long studentId, Pageable pageable);

    @Query("SELECT sc FROM StudentCourse sc WHERE sc.student.id = :studentId AND sc.isViewed = :isViewed")
    Page<StudentCourse> findByStudentIdAndIsViewed(@Param("studentId") Long studentId,
                                                   @Param("isViewed") Boolean isViewed,
                                                   Pageable pageable);

    @Query("SELECT sc FROM StudentCourse sc WHERE sc.student.id = :studentId AND sc.course.id = :courseId")
    Optional<StudentCourse> findByStudentIdAndCourseId(@Param("studentId") Long studentId,
                                                       @Param("courseId") Long courseId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}