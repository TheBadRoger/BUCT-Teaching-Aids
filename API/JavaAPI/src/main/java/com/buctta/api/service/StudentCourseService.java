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
     * 上报视频播放进度（断点续播）。
     * <p>
     * 若学生尚未选该课，会自动补一条选课记录，保证进度不丢。
     *
     * @param studentId    学生 ID
     * @param courseId     课程 ID
     * @param videoId      正在播放的视频 ID，可为空（仅更新课程级进度）
     * @param positionSeconds 当前播放位置（秒）
     * @param durationSeconds 视频总时长（秒），可为空；用于判断是否已看完
     */
    PlaybackProgressResult reportPlaybackProgress(Long studentId, Long courseId, Long videoId,
                                                  Integer positionSeconds, Integer durationSeconds);

    /** 查询续播位置；无记录时返回 null */
    PlaybackProgress getPlaybackProgress(Long studentId, Long courseId);

    /**
     * 续播位置视图。
     *
     * @param videoId        上次播放的视频，可为空
     * @param positionSeconds 上次播放位置（秒）
     * @param viewed         该课程是否已标记为已观看
     */
    record PlaybackProgress(Long videoId, Integer positionSeconds, Boolean viewed) {
    }

    /**
     * 播放进度上报结果
     *
     * @param exists 该学生是否已选该课程（走的是更新还是自动补建分支）
     */
    record PlaybackProgressResult(boolean success, boolean exists, StudentCourse studentCourse,
                                  String errorCode, String message) {
        public static PlaybackProgressResult success(StudentCourse studentCourse, boolean existed,
                                                     String message) {
            return new PlaybackProgressResult(true, existed, studentCourse, null, message);
        }

        public static PlaybackProgressResult fail(String errorCode, String message) {
            return new PlaybackProgressResult(false, false, null, errorCode, message);
        }
    }

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
