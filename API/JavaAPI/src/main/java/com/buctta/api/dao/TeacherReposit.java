package com.buctta.api.dao;

import com.buctta.api.entities.Teacher;
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

@Repository

public interface TeacherReposit extends JpaRepository<Teacher, Long>, JpaSpecificationExecutor<Teacher> {
    Teacher findTeacherListById(long Id);

    Teacher findTeacherListByName(String name);

    //Teacher findTeacherListByAddress(String address);
    @Modifying
    @Transactional
    @Query("DELETE FROM Teacher t WHERE t.id IN :ids")
    void deleteAllByIdIn(@Param("ids") List<Long> ids);

    // 新增：分页查询时同时 fetch user，避免 N+1 和懒加载异常
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Teacher> findAll(Specification<Teacher> spec, Pageable pageable);

    // 新增：无条件分页查询也 fetch user
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Teacher> findAll(Pageable pageable);

    // 新增：查询全部列表时 fetch user（用于导出）
    @Override
    @EntityGraph(attributePaths = {"user"})
    List<Teacher> findAll();
}