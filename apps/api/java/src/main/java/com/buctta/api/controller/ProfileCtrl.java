package com.buctta.api.controller;

import com.buctta.api.dto.UserProfileDTO;
import com.buctta.api.entities.User;
import com.buctta.api.service.ProfileService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资料读取（只读）。
 * <p>
 * 与 {@code /api/user/auth/current} 的区别：那个返回 Session 里的游离 User 实体，
 * 不含学生/教师详情；本模块在事务内重新加载并展开绑定信息与机构资料。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/profile")
public class ProfileCtrl {

    @Resource
    private ProfileService profileService;

    /** 当前登录用户的完整资料 */
    @GetMapping("/me")
    public ApiResponse<UserProfileDTO> me() {
        Long userId = currentUserId();
        if (userId == null) {
            return ApiResponse.fail(BusinessStatus.TOKEN_INVALID);
        }
        try {
            UserProfileDTO profile = profileService.byUserId(userId);
            if (profile == null) {
                // 账号存在但用户记录已被清理
                return ApiResponse.fail(BusinessStatus.USER_NOT_FOUND);
            }
            return ApiResponse.ok(profile);
        }
        catch (RuntimeException e) {
            log.error("读取当前用户资料失败 userId={}", userId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 按用户 ID 读取资料，用于查看他人主页。
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserProfileDTO> byUserId(@PathVariable Long userId) {
        try {
            UserProfileDTO profile = profileService.byUserId(userId);
            if (profile == null) {
                return ApiResponse.fail(BusinessStatus.USER_NOT_FOUND, "用户不存在，ID: " + userId);
            }
            return ApiResponse.ok(profile);
        }
        catch (RuntimeException e) {
            log.error("读取用户资料失败 userId={}", userId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    private Long currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
