package com.buctta.api.serviceimp;

import com.buctta.api.dto.LearningStatsDTO;
import com.buctta.api.dto.UserProfileDTO;
import com.buctta.api.entities.Organization;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.Teacher;
import com.buctta.api.entities.User;
import com.buctta.api.service.LearningStatsService;
import com.buctta.api.service.ProfileService;
import com.buctta.api.support.FakeRepositories;
import com.buctta.api.support.TestInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 用户资料读取：绑定信息展开、机构按名称解析。
 */
class ProfileServiceTest {

    private IMPL_ProfileService profileService;
    private FakeRepositories.FakeUserRepository users;
    private FakeRepositories.FakeStudentRepository students;
    private FakeRepositories.FakeOrganizationRepository organizations;

    @BeforeEach
    void setUp() {
        users = new FakeRepositories.FakeUserRepository();
        students = new FakeRepositories.FakeStudentRepository();
        organizations = new FakeRepositories.FakeOrganizationRepository();

        profileService = new IMPL_ProfileService();
        TestInjector.injectOrdered(profileService, users, students, organizations,
                new StubLearningStatsService());
    }

    @Test
    void byUserId_expandsStudentBinding() {
        Student student = new Student();
        student.setName("张三");
        student.setStudentNumber("2026001");
        Student saved = students.save(student);

        User user = new User();
        user.setUsername("alice");
        user.setPassword("x");
        user.setUserType(User.UserType.STUDENT);
        user.setStudent(saved);
        Long userId = users.save(user).getId();

        UserProfileDTO profile = profileService.byUserId(userId);

        assertNotNull(profile);
        assertEquals("STUDENT", profile.role());
        assertEquals("张三", profile.student().name());
        assertEquals("2026001", profile.student().studentNumber());
    }

    @Test
    void byUserId_resolvesOrganizationByName() {
        Organization organization = new Organization();
        organization.setName("信息科学与技术学院");
        organization.setLogo("/api/media/1/content");
        organizations.save(organization);

        User user = new User();
        user.setUsername("wang");
        user.setPassword("x");
        user.setUserType(User.UserType.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setName("王老师");
        teacher.setOrganization("信息科学与技术学院");
        user.setTeacher(teacher);
        Long userId = users.save(user).getId();

        UserProfileDTO profile = profileService.byUserId(userId);

        assertNotNull(profile.organization(), "机构应按 teacher.organization 名称解析出来");
        assertEquals("信息科学与技术学院", profile.organization().name());
        assertEquals("/api/media/1/content", profile.organization().logo());
    }

    @Test
    void byUserId_organizationNotRegistered_returnsNullOrganizationWithoutFailing() {
        User user = new User();
        user.setUsername("wang");
        user.setPassword("x");
        user.setUserType(User.UserType.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setName("王老师");
        teacher.setOrganization("尚未登记的学院");
        user.setTeacher(teacher);
        Long userId = users.save(user).getId();

        UserProfileDTO profile = profileService.byUserId(userId);

        assertNotNull(profile);
        assertEquals("王老师", profile.teacher().name(), "机构缺失不应影响教师资料本身");
        assertNull(profile.organization());
    }

    @Test
    void byUserId_teacherWithoutOrganizationName_skipsLookup() {
        User user = new User();
        user.setUsername("wang");
        user.setPassword("x");
        user.setUserType(User.UserType.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setName("王老师");
        user.setTeacher(teacher);
        Long userId = users.save(user).getId();

        assertNull(profileService.byUserId(userId).organization());
    }

    @Test
    void byUserId_missingUser_returnsNull() {
        assertNull(profileService.byUserId(999999L));
        assertNull(profileService.byUserId(null));
    }

    @Test
    void studentProfile_combinesProfileAndStats() {
        Student student = new Student();
        student.setName("张三");
        student.setStudentNumber("2026001");
        Student saved = students.save(student);

        User user = new User();
        user.setUsername("alice");
        user.setPassword("x");
        user.setUserType(User.UserType.STUDENT);
        user.setStudent(saved);
        users.save(user);

        ProfileService.StudentProfileView view = profileService.studentProfile(saved.getId());

        assertNotNull(view);
        assertEquals("张三", view.profile().student().name());
        assertNotNull(view.stats(), "应同时返回学习统计");
        assertEquals("STUDENT", view.stats().role());
        assertEquals(0, view.stats().courses().enrolled());
        assertEquals("start", view.stats().growth().key(), "无学习记录时等级为启程");
    }

    @Test
    void studentProfile_missingStudent_returnsNull() {
        assertNull(profileService.studentProfile(999999L));
        assertNull(profileService.studentProfile(null));
    }

    @Test
    void studentProfile_studentWithoutAccount_stillReturnsBasicProfile() {
        Student student = new Student();
        student.setName("孤立学生");
        student.setStudentNumber("2026999");
        Student saved = students.save(student);

        ProfileService.StudentProfileView view = profileService.studentProfile(saved.getId());

        assertNotNull(view);
        assertNull(view.profile().userId(), "未绑定账号时无 userId");
        assertEquals("孤立学生", view.profile().student().name());
        assertEquals("STUDENT", view.profile().role());
    }

    /**
     * 学习统计替身：本类只验证资料组装，统计部分给一个固定空视图即可，
     * 避免为了一个返回值构造 6 个仓储 fake。
     */
    private static class StubLearningStatsService implements LearningStatsService {

        @Override
        public LearningStatsDTO forStudent(Long studentId) {
            return new LearningStatsDTO("STUDENT", studentId, null, null,
                    new LearningStatsDTO.CourseMetrics(0, 0, 0d),
                    new LearningStatsDTO.NoteMetrics(0, 0, 0, 0),
                    new LearningStatsDTO.FollowMetrics(0, 0),
                    new LearningStatsDTO.GrowthStage("start", "启程", "加入第一门课程", 1, "稳固", 0d),
                    java.util.List.of(), java.util.List.of(), java.util.List.of());
        }

        @Override
        public LearningStatsDTO forTeacher(Long teacherId) {
            return null;
        }
    }
}
