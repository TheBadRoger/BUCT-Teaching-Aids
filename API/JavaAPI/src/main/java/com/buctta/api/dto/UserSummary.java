package com.buctta.api.dto;

import com.buctta.api.entities.User;

/**
 * 用户摘要：用于关注/粉丝列表等只需要展示身份的场景。
 * <p>
 * {@link #displayName} 优先取绑定的学生 / 教师真实姓名，未绑定时退回用户名，
 * 避免列表里出现一串没有意义的登录名。
 *
 * @param followedByMe 当前登录用户是否已关注该用户；无登录态时为 null
 */
public record UserSummary(Long id,
                          String username,
                          String displayName,
                          String avatar,
                          String userType,
                          Long studentId,
                          Long teacherId,
                          Boolean followedByMe) {

    public static UserSummary of(User user, Boolean followedByMe) {
        if (user == null) {
            return null;
        }
        String displayName = user.getUsername();
        Long studentId = null;
        Long teacherId = null;
        if (user.getStudent() != null) {
            studentId = user.getStudent().getId();
            if (user.getStudent().getName() != null && !user.getStudent().getName().isBlank()) {
                displayName = user.getStudent().getName();
            }
        }
        if (user.getTeacher() != null) {
            teacherId = user.getTeacher().getId();
            if (user.getTeacher().getName() != null && !user.getTeacher().getName().isBlank()) {
                displayName = user.getTeacher().getName();
            }
        }
        return new UserSummary(user.getId(),
                user.getUsername(),
                displayName,
                user.getAvatar(),
                user.getUserType() == null ? null : user.getUserType().name(),
                studentId,
                teacherId,
                followedByMe);
    }
}
