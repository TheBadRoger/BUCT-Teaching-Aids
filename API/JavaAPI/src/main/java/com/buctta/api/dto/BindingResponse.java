package com.buctta.api.dto;

import com.buctta.api.entities.Student;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户身份绑定响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BindingResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户类型
     */
    private User.UserType userType;

    /**
     * 绑定的学生信息（如果是学生）
     */
    private StudentInfo studentInfo;

    /**
     * 绑定的教师信息（如果是教师）
     */
    private TeacherInfo teacherInfo;

    public static BindingResponse from(User user, Student student, Teacher teacher) {
        return new BindingResponse(
                user.getId(),
                user.getUsername(),
                user.getUserType(),
                StudentInfo.from(student),
                TeacherInfo.from(teacher)
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        private Long id;
        private String studentNumber;
        private String name;
        private String className;
        private String gender;
        private String admissionDate;

        public static StudentInfo from(Student student) {
            if (student == null) return null;
            return new StudentInfo(
                    student.getId(),
                    student.getStudentNumber(),
                    student.getName(),
                    student.getClassName(),
                    student.getGender(),
                    student.getAdmissionDate() != null ? student.getAdmissionDate().toString() : null
            );
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherInfo {
        private Long id;
        private String name;
        private String organization;
        private String gender;
        private String education;
        private String jointime;

        public static TeacherInfo from(Teacher teacher) {
            if (teacher == null) return null;
            return new TeacherInfo(
                    teacher.getId(),
                    teacher.getName(),
                    teacher.getOrganization(),
                    teacher.getGender(),
                    teacher.getEducation(),
                    teacher.getJointime()
            );
        }
    }
}

