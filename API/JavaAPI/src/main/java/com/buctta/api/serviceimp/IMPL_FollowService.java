package com.buctta.api.serviceimp;

import com.buctta.api.dao.UserFollowRepository;
import com.buctta.api.dao.UserReposit;
import com.buctta.api.dto.UserSummary;
import com.buctta.api.entities.User;
import com.buctta.api.entities.UserFollow;
import com.buctta.api.service.FollowService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IMPL_FollowService implements FollowService {

    @Resource
    private UserFollowRepository userFollowRepository;

    @Resource
    private UserReposit userReposit;

    @Override
    @Transactional
    public FollowResult follow(Long followerId, Long followeeId) {
        FollowResult invalid = validate(followerId, followeeId);
        if (invalid != null) {
            return invalid;
        }
        if (userFollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return FollowResult.fail("ALREADY_FOLLOWED", "已经关注过该用户");
        }
        User follower = userReposit.findById(followerId).orElse(null);
        User followee = userReposit.findById(followeeId).orElse(null);
        if (follower == null || followee == null) {
            return FollowResult.fail("USER_NOT_FOUND", "用户不存在");
        }
        try {
            userFollowRepository.save(new UserFollow(follower, followee));
            return FollowResult.ok("关注成功");
        }
        catch (RuntimeException e) {
            // 并发下可能撞唯一约束，按"已关注"处理而不是 500
            log.warn("关注写入失败 followerId={} followeeId={}", followerId, followeeId, e);
            return FollowResult.fail("ALREADY_FOLLOWED", "已经关注过该用户");
        }
    }

    @Override
    @Transactional
    public FollowResult unfollow(Long followerId, Long followeeId) {
        FollowResult invalid = validate(followerId, followeeId);
        if (invalid != null) {
            return invalid;
        }
        if (!userFollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return FollowResult.fail("NOT_FOLLOWED", "尚未关注该用户");
        }
        try {
            userFollowRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
            return FollowResult.ok("已取消关注");
        }
        catch (RuntimeException e) {
            log.error("取消关注失败 followerId={} followeeId={}", followerId, followeeId, e);
            return FollowResult.fail("DELETE_FAILED", "取消关注失败: " + e.getMessage());
        }
    }

    @Override
    public Page<UserSummary> listFollowing(Long userId, Pageable pageable) {
        if (userId == null) {
            return Page.empty(pageable);
        }
        return toSummaryPage(userFollowRepository.findByFollowerId(userId, pageable),
                UserFollow::getFollowee, userId, pageable);
    }

    @Override
    public Page<UserSummary> listFollowers(Long userId, Pageable pageable) {
        if (userId == null) {
            return Page.empty(pageable);
        }
        return toSummaryPage(userFollowRepository.findByFolloweeId(userId, pageable),
                UserFollow::getFollower, userId, pageable);
    }

    @Override
    public FollowStats stats(Long userId) {
        if (userId == null) {
            return new FollowStats(0, 0);
        }
        return new FollowStats(userFollowRepository.countByFollowerId(userId),
                userFollowRepository.countByFolloweeId(userId));
    }

    @Override
    public List<Long> followingIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userFollowRepository.findFolloweeIds(userId);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return false;
        }
        return userFollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    /* ------------------------------------------------------------ */

    private FollowResult validate(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return FollowResult.fail("PARAM_MISSING", "关注者与被关注者不能为空");
        }
        if (followerId.equals(followeeId)) {
            return FollowResult.fail("SELF_FOLLOW", "不能关注自己");
        }
        return null;
    }

    /**
     * 把关注关系分页转为用户摘要分页。
     * <p>
     * 逐条 findById 会产生 N+1 查询，这里批量取回再按 ID 装配。
     */
    private Page<UserSummary> toSummaryPage(Page<UserFollow> relations,
                                            Function<UserFollow, User> picker,
                                            Long viewerId,
                                            Pageable pageable) {
        if (relations.isEmpty()) {
            return Page.empty(pageable);
        }
        List<Long> ids = relations.getContent().stream()
                .map(picker)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .toList();

        Map<Long, User> users = userReposit.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<UserSummary> summaries = new ArrayList<>(relations.getNumberOfElements());
        for (UserFollow relation : relations) {
            User user = picker.apply(relation);
            if (user == null) {
                continue;
            }
            User loaded = users.getOrDefault(user.getId(), user);
            summaries.add(UserSummary.of(loaded, true));
        }
        return new PageImpl<>(summaries, pageable, relations.getTotalElements());
    }
}
