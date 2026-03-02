package com.buctta.api.service;

import com.buctta.api.entities.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}