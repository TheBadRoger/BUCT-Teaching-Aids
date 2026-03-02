package com.buctta.api.service;

import com.buctta.api.entities.Student;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.User;

/**
 * 用户身份绑定服务接口
 */
public interface UserBindingService {

    /**
     * 绑定学生身份
     *
     * @param userId        用户ID
     * @param name          姓名
     * @param idCard        身份证号
     * @param studentNumber 学号
     * @return 绑定结果
     */
    BindingResult bindStudent(Long userId, String name, String idCard, String studentNumber);

    /**
     * 绑定教师身份
     *
     * @param userId         用户ID
     * @param name           姓名
     * @param idCard         身份证号
     * @param employeeNumber 工号
     * @return 绑定结果
     */
    BindingResult bindTeacher(Long userId, String name, String idCard, String employeeNumber);

    /**
     * 解绑身份
     *
     * @param userId 用户ID
     * @return 解绑结果
     */
    BindingResult unbind(Long userId);

    /**
     * 获取用户绑定的学生信息
     *
     * @param userId 用户ID
     * @return 学生信息，未绑定返回null
     */
    Student getBoundStudent(Long userId);

    /**
     * 获取用户绑定的教师信息
     *
     * @param userId 用户ID
     * @return 教师信息，未绑定返回null
     */
    Teacher getBoundTeacher(Long userId);

    /**
     * 绑定结果
     */
    record BindingResult(
            boolean success,
            String errorCode,
            String message,
            User user,
            Student student,
            Teacher teacher
    ) {
        public static BindingResult successStudent(User user, Student student) {
            return new BindingResult(true, null, "学生身份绑定成功", user, student, null);
        }

        public static BindingResult successTeacher(User user, Teacher teacher) {
            return new BindingResult(true, null, "教师身份绑定成功", user, null, teacher);
        }

        public static BindingResult successUnbind() {
            return new BindingResult(true, null, "解绑成功", null, null, null);
        }

        public static BindingResult fail(String errorCode, String message) {
            return new BindingResult(false, errorCode, message, null, null, null);
        }
    }
}

