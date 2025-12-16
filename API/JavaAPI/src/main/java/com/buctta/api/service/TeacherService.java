package com.buctta.api.service;

import com.buctta.api.entities.TeacherList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeacherService {
    TeacherList AddTeacher(TeacherList teacherList);
    Page<TeacherList> searchTeachers(String name, String organization, String jointime, String gender, String education, Pageable pageable);
}
