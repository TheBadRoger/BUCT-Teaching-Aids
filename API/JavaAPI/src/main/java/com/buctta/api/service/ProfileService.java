package com.buctta.api.service;

import com.buctta.api.dto.LearningStatsDTO;
import com.buctta.api.dto.UserProfileDTO;

/**
 * 用户资料读取（只读，不含修改）。
 */
public interface ProfileService {

    /**
     * 按用户 ID 取资料。
     * <p>
     * 必须在事务内重新加载用户，不能直接使用 Session 中的游离实体——
     * {@code User.student} / {@code User.teacher} 是懒加载关联，会话外读取会抛异常。
     *
     * @return 用户不存在时返回 null
     */
    UserProfileDTO byUserId(Long userId);

    /**
     * 学生资料 + 学习统计，供「学生-个人资料」页一次取齐。
     *
     * @return 学生不存在时返回 null
     */
    StudentProfileView studentProfile(Long studentId);

    /**
     * 学生资料视图。
     *
     * @param profile 用户资料（含学生信息）
     * @param stats   学习统计
     */
    record StudentProfileView(UserProfileDTO profile, LearningStatsDTO stats) {
    }
}
