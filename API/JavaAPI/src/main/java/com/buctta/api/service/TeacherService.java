package com.buctta.api.service;

import com.buctta.api.entities.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeacherService {
    Teacher AddTeacher(Teacher teacher);

    Page<Teacher> searchTeachers(String name, String organization, String jointime, String gender, String education, Pageable pageable);
}
