package com.buctta.api.service;

import com.buctta.api.entities.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface StudentService {

    // 添加学生
    Student addStudent(Student student);

    // 更新学生信息
    Student updateStudent(Long id, Student studentDetails);

    // 根据ID获取学生
    Student getStudentById(Long id);

    // 根据学号获取学生
    Student getStudentByNumber(String studentNumber);

    // 删除学生
    void deleteStudent(Long id);

    // 多条件搜索学生（分页）
    Page<Student> searchStudents(String name, String studentNumber, String className,
                                 String gender, String telephone, String email,
                                 Pageable pageable);

    // 获取所有学生（分页）
    Page<Student> getAllStudents(Pageable pageable);

    // 根据班级获取学生列表
    List<Student> getStudentsByClass(String className);

    // 检查学号是否存在
    boolean isStudentNumberExists(String studentNumber);

    // 根据入学年份获取学生
    List<Student> getStudentsByAdmissionYear(Integer year);
}