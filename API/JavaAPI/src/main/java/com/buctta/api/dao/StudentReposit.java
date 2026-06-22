package com.buctta.api.dao;

import com.buctta.api.entities.Student;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
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
    @Modifying
    @Transactional
    @Query("DELETE FROM Student s WHERE s.id IN :ids")
    void deleteAllByIdIn(@Param("ids") List<Long> ids);

    // 新增：分页查询时同时 fetch user
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Student> findAll(Specification<Student> spec, Pageable pageable);

    // 新增：无条件分页查询时 fetch user
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Student> findAll(Pageable pageable);

    // 新增：查询全部列表时 fetch user（用于导出）
    @Override
    @EntityGraph(attributePaths = {"user"})
    List<Student> findAll();
}