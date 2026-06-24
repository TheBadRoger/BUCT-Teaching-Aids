package com.buctta.api.service;

import com.buctta.api.dto.TeacherDTO;
import com.buctta.api.entities.Teacher;
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
}