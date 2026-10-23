package com.buctta.api.dto;

import com.buctta.api.entities.Organization;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.User;

/**
 * 用户资料聚合视图，服务「个人中心」「学生-个人资料」「老师-个人资料」「机构-个人资料」。
 * <p>
 * 之所以单独做一个接口：{@code /api/user/auth/current} 返回的是登录时放入 Session 的
 * 游离实体，不含头像之外的扩展字段、也不含绑定的学生/教师详情（且关联是懒加载，
 * 直接读取会抛 LazyInitializationException）。本接口在事务内重新加载，返回扁平可用结构。
 */
public record UserProfileDTO(
        Long userId,
        String username,
        String telephone,
        String email,
        String avatar,
        /** STUDENT / TEACHER / UNBOUND */
        String role,
        StudentProfile student,
        TeacherProfile teacher,
        /** 机构资料；未绑定教师或机构未登记时为 null */
        OrganizationProfile organization
) {

    /**
     * 学生资料
     */
    public record StudentProfile(Long id,
                                 String studentNumber,
                                 String name,
                                 String className,
                                 String gender,
                                 String admissionDate) {

        public static StudentProfile of(Student student) {
            if (student == null) {
                return null;
            }
            return new StudentProfile(student.getId(),
                    student.getStudentNumber(),
                    student.getName(),
                    student.getClassName(),
                    student.getGender(),
                    student.getAdmissionDate() == null ? null : student.getAdmissionDate().toString());
        }
    }

    /**
     * 教师资料
     */
    public record TeacherProfile(Long id,
                                 String name,
                                 String organization,
                                 String gender,
                                 String education,
                                 String jointime) {

        public static TeacherProfile of(Teacher teacher) {
            if (teacher == null) {
                return null;
            }
            return new TeacherProfile(teacher.getId(),
                    teacher.getName(),
                    teacher.getOrganization(),
                    teacher.getGender(),
                    teacher.getEducation(),
                    teacher.getJointime());
        }
    }

    /**
     * 机构资料（按 教师.organization ↔ 机构.name 关联）
     */
    public record OrganizationProfile(Long id,
                                      String name,
                                      String logo,
                                      String bannerUrl,
                                      String info,
                                      String honorCertUrl) {

        public static OrganizationProfile of(Organization organization) {
            if (organization == null) {
                return null;
            }
            return new OrganizationProfile(organization.getId(),
                    organization.getName(),
                    organization.getLogo(),
                    organization.getBannerUrl(),
                    organization.getInfo(),
                    organization.getHonorCertUrl());
        }
    }

    /**
     * 组装资料视图。
     *
     * @param user         已在事务内加载完成的用户实体（不可传入 Session 中的游离对象）
     * @param organization 已解析出的机构资料，可为 null
     */
    public static UserProfileDTO of(User user, Organization organization) {
        if (user == null) {
            return null;
        }
        Student student = user.getStudent();
        Teacher teacher = user.getTeacher();
        String role = user.getUserType() == null ? "UNBOUND" : user.getUserType().name();
        return new UserProfileDTO(user.getId(),
                user.getUsername(),
                user.getTelephone(),
                user.getEmail(),
                user.getAvatar(),
                role,
                StudentProfile.of(student),
                TeacherProfile.of(teacher),
                OrganizationProfile.of(organization));
    }
}
