package com.buctta.api.service;

import com.buctta.api.entities.CourseList;

import java.util.List;

/**
 * 课程访问量统计服务
 * 基于Redis的高并发处理和MySQL的持久化
 */
public interface CourseViewService {

    /**
     * 记录课程访问
     * 使用Redis的Sorted Set实时记录访问量，支持高并发
     *
     * @param courseId 课程ID
     */
    void recordCourseView(Long courseId);

    /**
     * 获取热门课程（前N个）
     *
     * @param limit 返回的课程数量，默认10
     * @return 按访问量降序排列的课程列表
     */
    List<CourseList> getPopularCourses(int limit);

    /**
     * 获取指定课程的访问量
     *
     * @param courseId 课程ID
     * @return 访问量数字
     */
    Long getCourseViewCount(Long courseId);

    /**
     * 将Redis中的访问数据同步到MySQL
     * 这个方法通常由定时任务调用
     */
    void syncViewCountsToDatabase();

    /**
     * 清除指定课程的访问记录
     *
     * @param courseId 课程ID
     */
    void clearCourseViewCount(Long courseId);

    /**
     * 重新构建Redis缓存
     * 从数据库读取所有课程的访问量到Redis中
     */
    void rebuildCourseViewCache();
}

