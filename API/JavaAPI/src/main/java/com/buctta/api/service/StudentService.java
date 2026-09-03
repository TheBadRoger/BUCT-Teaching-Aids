package com.buctta.api.service;

import com.buctta.api.dto.StudentDTO;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
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

    // 修改返回类型为 Page<StudentDTO>
    Page<StudentDTO> searchStudents(String name, String studentNumber, String className,
                                    String gender, String telephone, String email,
                                    Pageable pageable);

    // 新增：按条件返回 List<Student>，用于导出
    List<Student> searchStudentsBySpec(String name, String studentNumber, String className,
                                       String gender, String telephone, String email);

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

    StudentResult deleteStudents(List<Long> ids);

    List<Student> getAllStudentsForExport();

    byte[] exportStudentsToExcel(List<Student> students) throws IOException;
    /**
     * 新增学生，同时创建对应用户并绑定
     * @param student    学生基本信息
     * @param username   用户名（可选，默认使用学号）
     * @param password   密码（可选，不传则随机生成）
     * @param telephone  手机号（可选）
     * @param email      邮箱（可选）
     * @return 创建结果，包含学生和用户信息
     */
    AddWithUserResult addStudentWithUser(Student student, String username, String password, String telephone, String email);

    record AddWithUserResult(boolean success, Student student, User user, String errorCode, String message) {
        public static AddWithUserResult success(Student student, User user) {
            return new AddWithUserResult(true, student, user, null, "创建成功");
        }
        public static AddWithUserResult fail(String errorCode, String message) {
            return new AddWithUserResult(false, null, null, errorCode, message);
        }
    }
}