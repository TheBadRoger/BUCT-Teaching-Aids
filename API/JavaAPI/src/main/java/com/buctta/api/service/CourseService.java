package com.buctta.api.service;

import com.buctta.api.entities.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface CourseService {

    /**
     * 添加课程
     *
     * @param course 课程信息
     * @return 添加结果
     */
    CourseResult addCourse(Course course);

    /**
     * 搜索课程
     */
    Page<Course> searchCourses(String courseName, String courseNumber,
                               String teachingTeachers, String courseStatus,
                               String courseTags, String startDate, Pageable pageable);

    /**
     * 更新课程
     *
     * @param id            课程ID
     * @param courseDetails 课程详情
     * @return 更新结果
     */
    CourseResult updateCourse(Long id, Course courseDetails);

    /**
     * 获取课程详情
     *
     * @param id 课程ID
     * @return 课程信息，不存在返回null
     */
    Course getCourseById(Long id);

    /**
     * 删除课程
     *
     * @param id 课程ID
     * @return 删除结果
     */
    CourseResult deleteCourse(Long id);

    /**
     * 发布 / 取消发布课程（教师自助，无需审批）。
     * <p>
     * 归属校验为**弱校验**：课程的 {@code teachingTeachers} 是自由文本（逗号分隔的教师姓名），
     * 数据库层面没有教师外键，因此只能按姓名判断。传入 {@code teacherName} 为空时跳过归属校验
     * （供管理员调用）。
     *
     * @param id          课程 ID
     * @param published   目标发布状态
     * @param teacherName 调用者姓名，用于归属校验；为空表示不校验
     */
    CourseResult setPublished(Long id, Boolean published, String teacherName);

    /**
     * 课程操作结果
     */
    record CourseResult(boolean success, Course course, String errorCode, String message) {
        public static CourseResult success(Course course) {
            return new CourseResult(true, course, null, "操作成功");
        }

        public static CourseResult success(Course course, String message) {
            return new CourseResult(true, course, null, message);
        }

        public static CourseResult fail(String errorCode, String message) {
            return new CourseResult(false, null, errorCode, message);
        }
    }
    CourseResult deleteCourses(List<Long> ids);
    byte[] exportCoursesToExcel(List<Course> courses) throws IOException;
    List<Course> getAllCourses();
}