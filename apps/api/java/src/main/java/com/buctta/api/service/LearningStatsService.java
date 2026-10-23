package com.buctta.api.service;

import com.buctta.api.dto.LearningStatsDTO;

/**
 * 学习统计聚合：同时服务「成长地图」与「学习分布」。
 * <p>
 * 设计意图是把原先散落在前端的折算规则（完成率、成长等级、标签分布、活跃度趋势）
 * 收到后端一处，保证各页面口径一致、可追溯。
 */
public interface LearningStatsService {

    /**
     * 学生维度的学习统计。
     *
     * @param studentId 学生 ID
     * @return 聚合结果；学生不存在时返回 null
     */
    LearningStatsDTO forStudent(Long studentId);

    /**
     * 教师维度的教研统计（开课数、教参产出等）。
     *
     * @param teacherId 教师 ID
     */
    LearningStatsDTO forTeacher(Long teacherId);
}
