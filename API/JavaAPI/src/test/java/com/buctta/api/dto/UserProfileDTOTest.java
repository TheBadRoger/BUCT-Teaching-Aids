package com.buctta.api.dto;

import com.buctta.api.entities.Organization;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 用户资料聚合：绑定信息展开与角色推导。
 */
class UserProfileDTOTest {

    @Test
    void of_studentUser_exposesStudentProfileAndRole() {
        User user = baseUser("alice");
        user.setUserType(User.UserType.STUDENT);
        Student student = new Student();
        student.setId(11L);
        student.setName("张三");
        student.setStudentNumber("2026001");
        student.setClassName("计科2601");
        student.setGender("男");
        student.setAdmissionDate(LocalDate.of(2026, 9, 1));
        user.setStudent(student);

        UserProfileDTO dto = UserProfileDTO.of(user, null);

        assertEquals("STUDENT", dto.role());
        assertEquals(11L, dto.student().id());
        assertEquals("张三", dto.student().name());
        assertEquals("2026-09-01", dto.student().admissionDate());
        assertNull(dto.teacher(), "学生账号不应带教师资料");
        assertNull(dto.organization(), "未绑定教师时无机构资料");
    }

    @Test
    void of_teacherUser_resolvesOrganizationProfile() {
        User user = baseUser("wang");
        user.setUserType(User.UserType.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setId(3L);
        teacher.setName("王老师");
        teacher.setOrganization("信息科学与技术学院");
        teacher.setEducation("博士");
        user.setTeacher(teacher);

        Organization organization = new Organization();
        organization.setId(5L);
        organization.setName("信息科学与技术学院");
        organization.setBannerUrl("/api/media/9/content");

        UserProfileDTO dto = UserProfileDTO.of(user, organization);

        assertEquals("TEACHER", dto.role());
        assertEquals("王老师", dto.teacher().name());
        assertEquals("信息科学与技术学院", dto.teacher().organization());
        assertEquals(5L, dto.organization().id());
        assertEquals("/api/media/9/content", dto.organization().bannerUrl());
        assertNull(dto.student(), "教师账号不应带学生资料");
    }

    @Test
    void of_unboundUser_hasUnboundRoleAndNoBindings() {
        User user = baseUser("guest");

        UserProfileDTO dto = UserProfileDTO.of(user, null);

        assertEquals("UNBOUND", dto.role(), "未绑定身份时应为 UNBOUND 而不是 null");
        assertNull(dto.student());
        assertNull(dto.teacher());
        assertNull(dto.organization());
    }

    @Test
    void of_carriesAvatarAndContactFields() {
        User user = baseUser("alice");
        user.setAvatar("/api/media/7/content");
        user.setTelephone("13800000000");
        user.setEmail("a@b.c");

        UserProfileDTO dto = UserProfileDTO.of(user, null);

        assertEquals("/api/media/7/content", dto.avatar());
        assertEquals("13800000000", dto.telephone());
        assertEquals("a@b.c", dto.email());
    }

    @Test
    void of_nullUser_returnsNull() {
        assertNull(UserProfileDTO.of(null, null));
    }

    @Test
    void of_teacherWithoutOrganizationRecord_stillReturnsTeacherProfile() {
        User user = baseUser("wang");
        user.setUserType(User.UserType.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setId(3L);
        teacher.setName("王老师");
        teacher.setOrganization("尚未登记的学院");
        user.setTeacher(teacher);

        // 机构未发布资料时按名称解析不到，此时不应影响教师资料本身
        UserProfileDTO dto = UserProfileDTO.of(user, null);

        assertEquals("王老师", dto.teacher().name());
        assertEquals("尚未登记的学院", dto.teacher().organization());
        assertNull(dto.organization());
    }

    @Test
    void studentProfile_mapsNullableFieldsSafely() {
        User user = baseUser("alice");
        Student student = new Student();
        student.setId(11L);
        student.setName("张三");

        user.setStudent(student);
        UserProfileDTO dto = UserProfileDTO.of(user, null);

        assertNull(dto.student().studentNumber());
        assertNull(dto.student().className());
        assertNull(dto.student().admissionDate(), "未填入学日期时不应抛异常");
    }

    private User baseUser(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword("x");
        return user;
    }
}
