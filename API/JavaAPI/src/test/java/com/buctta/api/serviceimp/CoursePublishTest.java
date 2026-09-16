package com.buctta.api.serviceimp;

import com.buctta.api.entities.Course;
import com.buctta.api.service.CourseService;
import com.buctta.api.support.FakeRepositories;
import com.buctta.api.support.TestInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课程自助发布（无需审批）。
 * <p>
 * 归属校验是**弱校验**：课程的授课教师存为逗号分隔的姓名字符串，没有教师外键，
 * 因此按姓名包含匹配。这些用例把这个语义固定下来。
 */
class CoursePublishTest {

    private IMPL_CourseService service;
    private FakeRepositories.FakeCourseReposit courses;

    private Long courseId;

    @BeforeEach
    void setUp() {
        courses = new FakeRepositories.FakeCourseReposit();
        service = new IMPL_CourseService();
        TestInjector.injectOrdered(service, courses);
        courseId = createCourse("数据结构", "CS101", "王老师");
    }

    @Test
    void publish_byOwningTeacher_succeeds() {
        CourseService.CourseResult result = service.setPublished(courseId, true, "王老师");

        assertTrue(result.success());
        assertTrue(courses.findById(courseId).orElseThrow().getPublished());
        assertEquals("课程已发布", result.message());
    }

    @Test
    void publish_byAnotherTeacher_isRejected() {
        CourseService.CourseResult result = service.setPublished(courseId, true, "李老师");

        assertFalse(result.success());
        assertEquals("NO_PERMISSION", result.errorCode());
        assertFalse(courses.findById(courseId).orElseThrow().getPublished(),
                "越权发布不应生效");
    }

    @Test
    void unpublish_togglesBackOff() {
        service.setPublished(courseId, true, "王老师");
        CourseService.CourseResult result = service.setPublished(courseId, false, "王老师");

        assertTrue(result.success());
        assertFalse(courses.findById(courseId).orElseThrow().getPublished());
        assertEquals("课程已取消发布", result.message());
    }

    @Test
    void publishedDefaultsToTrueWhenNull() {
        CourseService.CourseResult result = service.setPublished(courseId, null, "王老师");

        assertTrue(result.success());
        assertTrue(courses.findById(courseId).orElseThrow().getPublished());
    }

    @Test
    void adminBypass_skipsOwnershipCheck() {
        // teacherName 为 null 表示管理员登录态
        CourseService.CourseResult result = service.setPublished(courseId, true, null);

        assertTrue(result.success(), "管理员应可发布任意课程");
        assertTrue(courses.findById(courseId).orElseThrow().getPublished());
    }

    @Test
    void unmatchedCaller_isRejected() {
        CourseService.CourseResult result = service.setPublished(courseId, true, "");

        assertFalse(result.success());
        assertEquals("NO_PERMISSION", result.errorCode());
    }

    @Test
    void multipleTeachers_anyOfThemCanPublish() {
        Long shared = createCourse("操作系统", "CS102", "张三、李四, 王五");

        assertTrue(service.setPublished(shared, true, "李四").success());
        assertFalse(service.setPublished(shared, false, "赵六").success());
    }

    @Test
    void teacherWithSuffix_stillMatches() {
        Long course = createCourse("编译原理", "CS103", "王老师（教授）");

        assertTrue(service.setPublished(course, true, "王老师").success(),
                "授课教师写法可能带后缀，应做包含匹配");
    }

    @Test
    void emptyTeachingTeachers_doesNotBlock() {
        Long unassigned = createCourse("待定课程", "CS104", null);

        assertTrue(service.setPublished(unassigned, true, "任意老师").success(),
                "未填写授课教师时不应把流程卡死");
    }

    @Test
    void missingCourse_returnsNotFound() {
        CourseService.CourseResult result = service.setPublished(999999L, true, "王老师");

        assertFalse(result.success());
        assertEquals("COURSE_NOT_FOUND", result.errorCode());
    }

    @Test
    void nullCourseId_returnsParamMissing() {
        CourseService.CourseResult result = service.setPublished(null, true, "王老师");

        assertFalse(result.success());
        assertEquals("PARAM_MISSING", result.errorCode());
    }

    private Long createCourse(String name, String number, String teachingTeachers) {
        Course course = new Course();
        course.setCourseName(name);
        course.setCourseNumber(number);
        course.setTeachingTeachers(teachingTeachers);
        return courses.save(course).getId();
    }
}
