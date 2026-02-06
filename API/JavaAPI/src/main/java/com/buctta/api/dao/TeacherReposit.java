package com.buctta.api.dao;

import com.buctta.api.entities.TeacherList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository

public interface TeacherReposit extends JpaRepository<TeacherList, Long>, JpaSpecificationExecutor<TeacherList> {
    TeacherList findTeacherListById(long Id);

    TeacherList findTeacherListByName(String name);

    TeacherList findTeacherListByEmail(String email);

    TeacherList findTeacherListByTelephone(String telephone);
    //TeacherList findTeacherListByAddress(String address);
}
