package com.buctta.api.controller;

import com.buctta.api.dto.UserSummary;
import com.buctta.api.entities.User;
import com.buctta.api.service.FollowService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关注关系接口，为「好友动态」提供关注范围。
 * <p>
 * 关注动作的操作者一律取自当前登录态，不接受前端传入自己的 ID，
 * 避免越权替他人建立关注关系。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/follow")
public class FollowController {

    @Resource
    private FollowService followService;

    /** 关注某人 */
    @PostMapping("/{followeeId}")
    public ApiResponse<String> follow(@PathVariable Long followeeId) {
        Long currentUserId = currentUserId();
        if (currentUserId == null) {
            return ApiResponse.fail(BusinessStatus.TOKEN_INVALID);
        }
        FollowService.FollowResult result = followService.follow(currentUserId, followeeId);
        return toResponse(result);
    }

    /** 取消关注 */
    @DeleteMapping("/{followeeId}")
    public ApiResponse<String> unfollow(@PathVariable Long followeeId) {
        Long currentUserId = currentUserId();
        if (currentUserId == null) {
            return ApiResponse.fail(BusinessStatus.TOKEN_INVALID);
        }
        return toResponse(followService.unfollow(currentUserId, followeeId));
    }

    /** 我关注的人 */
    @GetMapping("/following")
    public ApiResponse<Page<UserSummary>> listFollowing(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = currentUserId();
        if (currentUserId == null) {
            return ApiResponse.fail(BusinessStatus.TOKEN_INVALID);
        }
        try {
            return ApiResponse.ok(followService.listFollowing(currentUserId, pageable(page, size)));
        }
        catch (Exception e) {
            log.error("查询关注列表失败 userId={}", currentUserId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /** 关注我的人 */
    @GetMapping("/followers")
    public ApiResponse<Page<UserSummary>> listFollowers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = currentUserId();
        if (currentUserId == null) {
            return ApiResponse.fail(BusinessStatus.TOKEN_INVALID);
        }
        try {
            return ApiResponse.ok(followService.listFollowers(currentUserId, pageable(page, size)));
        }
        catch (Exception e) {
            log.error("查询粉丝列表失败 userId={}", currentUserId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /** 关注统计 + 我与指定用户的关系，供个人主页/动态页初始化 */
    @GetMapping("/stats")
    public ApiResponse<FollowStatsResponse> stats(
            @RequestParam(required = false) Long userId) {
        Long currentUserId = currentUserId();
        Long targetId = userId != null ? userId : currentUserId;
        if (targetId == null) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "未登录时必须提供 userId");
        }
        try {
            FollowService.FollowStats stats = followService.stats(targetId);
            Boolean followedByMe = currentUserId == null
                    ? null
                    : followService.isFollowing(currentUserId, targetId);
            return ApiResponse.ok(new FollowStatsResponse(targetId, stats.following(),
                    stats.followers(), followedByMe));
        }
        catch (Exception e) {
            log.error("查询关注统计失败 userId={}", targetId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /** 我关注的人的 ID 列表，供动态流按关注范围过滤 */
    @GetMapping("/following-ids")
    public ApiResponse<List<Long>> followingIds() {
        Long currentUserId = currentUserId();
        if (currentUserId == null) {
            return ApiResponse.fail(BusinessStatus.TOKEN_INVALID);
        }
        return ApiResponse.ok(followService.followingIds(currentUserId));
    }

    /* ------------------------------------------------------------ */

    private Pageable pageable(int page, int size) {
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }

    private ApiResponse<String> toResponse(FollowService.FollowResult result) {
        if (result.success()) {
            return ApiResponse.ok(result.message());
        }
        return ApiResponse.fail(FollowService.statusOf(result.errorCode()), result.message());
    }

    private Long currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    /**
     * 关注统计响应。
     *
     * @param followedByMe 当前登录用户是否已关注该用户；未登录为 null
     */
    public record FollowStatsResponse(Long userId, long following, long followers,
                                      Boolean followedByMe) {
    }
}
