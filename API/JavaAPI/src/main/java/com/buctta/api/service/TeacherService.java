package com.buctta.api.service;

import com.buctta.api.dto.TeacherDTO;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface TeacherService {

    TeacherResult addTeacher(Teacher teacher);

    // 修改返回类型为 Page<TeacherDTO>
    Page<TeacherDTO> searchTeachers(String name, String organization, String jointime,
                                    String gender, String education,
                                    String username, String telephone, String email,
                                    String userType,
                                    Pageable pageable);

    // 新增：按相同条件直接返回 List<Teacher>，用于导出
    List<Teacher> searchTeachersBySpec(String name, String organization, String jointime,
                                       String gender, String education,
                                       String username, String telephone, String email,
                                       String userType);
    record TeacherResult(boolean success, Teacher teacher, String errorCode, String message) {
        public static TeacherResult success(Teacher teacher) {
            return new TeacherResult(true, teacher, null, "操作成功");
        }

        public static TeacherResult success(Teacher teacher, String message) {
            return new TeacherResult(true, teacher, null, message);
        }

        public static TeacherResult fail(String errorCode, String message) {
            return new TeacherResult(false, null, errorCode, message);
        }
    }

    TeacherResult deleteTeachers(List<Long> ids);

    byte[] exportTeachersToExcel(List<Teacher> teachers) throws IOException;

    // 保留，用于全量导出（可不调用，保留向后兼容）
    List<Teacher> getAllTeachersForExport();

    TeacherResult updateTeacher(Long id, Teacher teacherDetails);
    /**
     * 新增教师，同时创建对应用户并绑定
     * @param teacher    教师基本信息
     * @param username   用户名（可选，默认使用工号）
     * @param password   密码（可选，不传则随机生成）
     * @param telephone  手机号（可选）
     * @param email      邮箱（可选）
     * @return 创建结果，包含教师和用户信息
     */
    AddWithUserResult addTeacherWithUser(Teacher teacher, String username, String password, String telephone, String email);

    record AddWithUserResult(boolean success, Teacher teacher, User user, String errorCode, String message) {
        public static AddWithUserResult success(Teacher teacher, User user) {
            return new AddWithUserResult(true, teacher, user, null, "创建成功");
        }
        public static AddWithUserResult fail(String errorCode, String message) {
            return new AddWithUserResult(false, null, null, errorCode, message);
        }
    }
}