package com.buctta.api.service;

import com.buctta.api.entities.StudentCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentCourseService {

    /**
     * 选课
     *
     * @param studentId 学生ID
     * @param courseId  课程ID
     * @return 选课结果
     */
    CourseOperationResult selectCourse(Long studentId, Long courseId);

    /**
     * 更新观看状态
     *
     * @param studentId 学生ID
     * @param courseId  课程ID
     * @param isViewed  是否已观看
     * @return 更新结果
     */
    CourseOperationResult updateViewedStatus(Long studentId, Long courseId, Boolean isViewed);

    /**
     * 获取学生所有课程
     */
    Page<StudentCourse> getAllCourses(Long studentId, Pageable pageable);

    /**
     * 获取已观看课程
     */
    Page<StudentCourse> getViewedCourses(Long studentId, Pageable pageable);

    /**
     * 获取未观看课程
     */
    Page<StudentCourse> getNotViewedCourses(Long studentId, Pageable pageable);

    /**
     * 退课
     *
     * @param studentId 学生ID
     * @param courseId  课程ID
     * @return 退课结果
     */
    CourseOperationResult dropCourse(Long studentId, Long courseId);

    /**
     * 搜索学生课程
     */
    Page<StudentCourse> searchStudentCourses(String studentName, String courseName,
                                             Boolean isViewed, Long studentId, Pageable pageable);

    /**
     * 课程操作结果
     */
    record CourseOperationResult(boolean success, StudentCourse studentCourse, String errorCode, String message) {
        public static CourseOperationResult success(StudentCourse studentCourse) {
            return new CourseOperationResult(true, studentCourse, null, "操作成功");
        }

        public static CourseOperationResult success(StudentCourse studentCourse, String message) {
            return new CourseOperationResult(true, studentCourse, null, message);
        }

        public static CourseOperationResult fail(String errorCode, String message) {
            return new CourseOperationResult(false, null, errorCode, message);
        }
    }
}
