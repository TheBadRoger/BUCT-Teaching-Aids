package com.buctta.api.dao;

import com.buctta.api.entities.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository

public interface TeacherReposit extends JpaRepository<Teacher, Long>, JpaSpecificationExecutor<Teacher> {
    Teacher findTeacherListById(long Id);

    Teacher findTeacherListByName(String name);

    Teacher findTeacherListByEmail(String email);

    Teacher findTeacherListByTelephone(String telephone);
    //Teacher findTeacherListByAddress(String address);
}
