package com.buctta.api.dto;

import java.util.List;

/**
 * 学习统计聚合视图，同时服务「成长地图」与「学习分布」两个页面。
 * <p>
 * 之所以合成一个接口：两个页面取的是同一批数据的不同切面，各自拆成独立接口会导致
 * 前端为渲染一页发多次请求，且聚合口径容易出现两套。
 */
public record LearningStatsDTO(
        /** STUDENT / TEACHER / UNBOUND */
        String role,
        Long studentId,
        Long teacherId,
        /** 学生的展示名，未绑定时为用户名 */
        String displayName,

        CourseMetrics courses,
        NoteMetrics notes,
        FollowMetrics follows,

        GrowthStage growth,

        /** 学习活跃度：按课程标签聚合 */
        List<DistributionItem> tagDistribution,
        /** 活跃度趋势：按创建月份聚合 */
        List<ActivityPoint> activityByMonth,
        /** 笔记产出分布：按课程聚合 */
        List<DistributionItem> notesByCourse
) {

    /**
     * 课程维度指标
     *
     * @param enrolled   已选课程数
     * @param completed  已完成（已观看）课程数
     * @param completionRate 完成率，0~1，无选课时为 0
     */
    public record CourseMetrics(long enrolled, long completed, double completionRate) {
    }

    /**
     * 笔记维度指标
     *
     * @param total     笔记总数
     * @param publicCount 公开笔记数
     * @param totalLikes 收到的总点赞数
     * @param totalComments 收到的总评论数
     */
    public record NoteMetrics(long total, long publicCount, long totalLikes, long totalComments) {
    }

    /**
     * 关注维度指标
     *
     * @param following 我关注的人数
     * @param followers 关注我的人数
     */
    public record FollowMetrics(long following, long followers) {
    }

    /**
     * 成长等级。
     * <p>
     * 由后端统一计算，避免各页面自己编一套折算规则导致口径不一致。
     *
     * @param key        阶段标识：start / steady / deep / expert
     * @param title      阶段名称
     * @param description 达成条件描述
     * @param achieved   当前已达到的最高阶段序号（从 1 开始）
     * @param nextTitle  下一阶段名称；已是最高阶段时为 null
     * @param progressToNext 距离下一阶段的完成度 0~1；已是最高阶段时为 1
     */
    public record GrowthStage(String key, String title, String description,
                              int achieved, String nextTitle, double progressToNext) {
    }

    /**
     * 分布项
     *
     * @param label 标签名，如课程标签、课程名
     * @param count 数量
     * @param ratio 占比 0~1
     */
    public record DistributionItem(String label, long count, double ratio) {
    }

    /**
     * 活跃度数据点
     *
     * @param month 月份，格式 yyyy-MM
     * @param count 该月笔记数
     */
    public record ActivityPoint(String month, long count) {
    }
}
