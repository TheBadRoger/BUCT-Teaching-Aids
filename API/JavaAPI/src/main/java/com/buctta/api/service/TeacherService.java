package com.buctta.api.service;

import com.buctta.api.entities.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface TeacherService {
    /**
     * 添加教师
     *
     * @param teacher 教师信息
     * @return 添加结果
     */
    TeacherResult addTeacher(Teacher teacher);

    /**
     * 搜索教师
     */
    Page<Teacher> searchTeachers(String name, String organization, String jointime,
                                 String gender, String education, Pageable pageable);

    /**
     * 教师操作结果
     */
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
    List<Teacher> getAllTeachersForExport();

    TeacherResult updateTeacher(Long id, Teacher teacherDetails);
}
