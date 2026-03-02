package com.buctta.api.service;

import com.buctta.api.entities.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentService {

    /**
     * 添加学生
     *
     * @param student 学生信息
     * @return 添加结果
     */
    StudentResult addStudent(Student student);

    /**
     * 更新学生信息
     *
     * @param id             学生ID
     * @param studentDetails 学生详情
     * @return 更新结果
     */
    StudentResult updateStudent(Long id, Student studentDetails);

    /**
     * 获取学生信息
     *
     * @param id 学生ID
     * @return 学生信息，不存在返回null
     */
    Student getStudentById(Long id);

    /**
     * 根据学号获取学生
     *
     * @param studentNumber 学号
     * @return 学生信息，不存在返回null
     */
    Student getStudentByNumber(String studentNumber);

    /**
     * 删除学生
     *
     * @param id 学生ID
     * @return 删除结果
     */
    StudentResult deleteStudent(Long id);

    Page<Student> searchStudents(String name, String studentNumber, String className,
                                 String gender, String telephone, String email,
                                 Pageable pageable);

    Page<Student> getAllStudents(Pageable pageable);

    List<Student> getStudentsByClass(String className);

    boolean isStudentNumberExists(String studentNumber);

    List<Student> getStudentsByAdmissionYear(Integer year);

    /**
     * 学生操作结果
     */
    record StudentResult(boolean success, Student student, String errorCode, String message) {
        public static StudentResult success(Student student) {
            return new StudentResult(true, student, null, "操作成功");
        }

        public static StudentResult success(Student student, String message) {
            return new StudentResult(true, student, null, message);
        }

        public static StudentResult fail(String errorCode, String message) {
            return new StudentResult(false, null, errorCode, message);
        }
    }
}