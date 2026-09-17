package com.buctta.api.service;

import com.buctta.api.dto.UserSummary;
import com.buctta.api.utils.BusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 关注关系：好友动态的基础。
 */
public interface FollowService {

    /**
     * 关注某人。
     *
     * @return 操作结果；重复关注返回失败（错误码 ALREADY_FOLLOWED）
     */
    FollowResult follow(Long followerId, Long followeeId);

    /** 取消关注 */
    FollowResult unfollow(Long followerId, Long followeeId);

    /** 我关注的人 */
    Page<UserSummary> listFollowing(Long userId, Pageable pageable);

    /** 关注我的人 */
    Page<UserSummary> listFollowers(Long userId, Pageable pageable);

    /** 关注统计 */
    FollowStats stats(Long userId);

    /**
     * 我关注的人的 ID 列表，供动态流按关注范围过滤。
     * 无关注时返回空列表（调用方应据此返回空动态，而不是退回全站内容）。
     */
    List<Long> followingIds(Long userId);

    boolean isFollowing(Long followerId, Long followeeId);

    /**
     * 关注统计结果
     *
     * @param following 我关注的人数
     * @param followers 关注我的人数
     */
    record FollowStats(long following, long followers) {
    }

    /**
     * 关注操作结果
     *
     * @param errorCode 失败时的业务标识，如 SELF_FOLLOW / USER_NOT_FOUND / ALREADY_FOLLOWED
     */
    record FollowResult(boolean success, String errorCode, String message) {
        public static FollowResult ok(String message) {
            return new FollowResult(true, null, message);
        }

        public static FollowResult fail(String errorCode, String message) {
            return new FollowResult(false, errorCode, message);
        }
    }

    /** 把 errorCode 映射为对外业务状态码 */
    static BusinessStatus statusOf(String errorCode) {
        if (errorCode == null) {
            return BusinessStatus.INTERNAL_ERROR;
        }
        return switch (errorCode) {
            case "SELF_FOLLOW" -> BusinessStatus.PARAM_FORMAT_ERROR;
            case "USER_NOT_FOUND" -> BusinessStatus.USER_NOT_FOUND;
            case "ALREADY_FOLLOWED" -> BusinessStatus.ALREADY_FOLLOWED;
            case "NOT_FOLLOWED" -> BusinessStatus.NOT_FOLLOWED;
            default -> BusinessStatus.INTERNAL_ERROR;
        };
    }
}
