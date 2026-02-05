package com.buctta.api.service;

import com.buctta.api.entities.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface StudentService {
    Student addStudent(Student student);

    Student updateStudent(Long id, Student studentDetails);

    Student getStudentById(Long id);

    Student getStudentByNumber(String studentNumber);

    void deleteStudent(Long id);

    Page<Student> searchStudents(String name, String studentNumber, String className,
                                 String gender, String telephone, String email,
                                 Pageable pageable);

    Page<Student> getAllStudents(Pageable pageable);

    List<Student> getStudentsByClass(String className);

    boolean isStudentNumberExists(String studentNumber);

    List<Student> getStudentsByAdmissionYear(Integer year);
}