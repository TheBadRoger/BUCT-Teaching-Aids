package com.buctta.api.dao;

import com.buctta.api.entities.Teacher;
import jakarta.transaction.Transactional;
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

}
