package com.buctta.api.dao;

import com.buctta.api.entities.Student;
import com.buctta.api.entities.StudentCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentReposit extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    Optional<Student> findByStudentNumber(String studentNumber);
    List<Student> findByNameContaining(String name);
    List<Student> findByClassName(String className);
    List<Student> findByGender(String gender);
    boolean existsByStudentNumber(String studentNumber);
    Optional<Student> findByEmail(String email);
    Optional<Student> findByTelephone(String telephone);
    Optional<Student> findById(Long id);

}