package com.buctta.api.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 课程热门排行响应对象
 * 返回给前端的热门课程信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoursePopularityResponse {

    // 排行列表
    private List<CoursePopularityItem> items;

    // 统计信息
    private PopularityStats stats;

    // 更新时间戳
    private Long timestamp;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CoursePopularityItem {
        // 排名
        private Integer ranking;

        // 课程ID
        private Long courseId;

        // 课程名称
        private String courseName;

        // 访问量
        private Long viewCount;

        // 课程状态
        private String courseStatus;

        // 课程标签
        private String courseTags;

        // 课程图片
        private String courseImage;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PopularityStats {
        // 总课程数
        private Integer totalCourses;

        // 总访问量
        private Long totalViews;

        // 平均访问量
        private Long averageViews;

        // 最高访问量
        private Long maxViews;
    }
}

