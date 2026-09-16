package com.buctta.api.serviceimp;

import com.buctta.api.dto.UserSummary;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.User;
import com.buctta.api.service.FollowService;
import com.buctta.api.support.FakeRepositories;
import com.buctta.api.support.TestInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 关注关系：好友动态的数据基础。
 */
class FollowServiceTest {

    private IMPL_FollowService followService;
    private FakeRepositories.FakeUserRepository users;
    private FakeRepositories.FakeUserFollowRepository follows;

    private Long aliceId;
    private Long bobId;
    private Long carolId;

    @BeforeEach
    void setUp() {
        users = new FakeRepositories.FakeUserRepository();
        follows = new FakeRepositories.FakeUserFollowRepository();
        followService = new IMPL_FollowService();
        TestInjector.injectOrdered(followService, follows, users);

        aliceId = createUser("alice", null);
        bobId = createUser("bob", "张三");
        carolId = createUser("carol", "李四");
    }

    @Test
    void follow_createsRelationAndReflectsInStats() {
        FollowService.FollowResult result = followService.follow(aliceId, bobId);
        assertTrue(result.success());

        FollowService.FollowStats stats = followService.stats(aliceId);
        assertEquals(1, stats.following());
        assertEquals(0, stats.followers());

        FollowService.FollowStats bobStats = followService.stats(bobId);
        assertEquals(0, bobStats.following());
        assertEquals(1, bobStats.followers());
    }

    @Test
    void follow_isDirectional() {
        followService.follow(aliceId, bobId);

        assertTrue(followService.isFollowing(aliceId, bobId));
        assertFalse(followService.isFollowing(bobId, aliceId), "关注应是单向的");
    }

    @Test
    void follow_rejectsSelfFollow() {
        FollowService.FollowResult result = followService.follow(aliceId, aliceId);

        assertFalse(result.success());
        assertEquals("SELF_FOLLOW", result.errorCode());
        assertEquals(0, followService.stats(aliceId).following());
    }

    @Test
    void follow_rejectsDuplicate() {
        followService.follow(aliceId, bobId);
        FollowService.FollowResult again = followService.follow(aliceId, bobId);

        assertFalse(again.success(), "重复关注应被拒绝而不是静默成功");
        assertEquals("ALREADY_FOLLOWED", again.errorCode());
        assertEquals(1, followService.stats(aliceId).following(), "不应产生第二条关系");
    }

    @Test
    void follow_rejectsMissingUser() {
        FollowService.FollowResult result = followService.follow(aliceId, 999999L);

        assertFalse(result.success());
        assertEquals("USER_NOT_FOUND", result.errorCode());
    }

    @Test
    void follow_rejectsNullArguments() {
        assertFalse(followService.follow(null, bobId).success());
        assertFalse(followService.follow(aliceId, null).success());
    }

    @Test
    void unfollow_removesRelation() {
        followService.follow(aliceId, bobId);

        FollowService.FollowResult result = followService.unfollow(aliceId, bobId);
        assertTrue(result.success());
        assertFalse(followService.isFollowing(aliceId, bobId));
        assertEquals(0, followService.stats(aliceId).following());
    }

    @Test
    void unfollow_whenNotFollowing_returnsFailure() {
        FollowService.FollowResult result = followService.unfollow(aliceId, carolId);

        assertFalse(result.success());
        assertEquals("NOT_FOLLOWED", result.errorCode());
    }

    @Test
    void followingIds_returnsOnlyMyFollowees() {
        followService.follow(aliceId, bobId);
        followService.follow(aliceId, carolId);
        followService.follow(bobId, carolId);

        List<Long> aliceFollowing = followService.followingIds(aliceId);

        assertEquals(2, aliceFollowing.size());
        assertTrue(aliceFollowing.contains(bobId));
        assertTrue(aliceFollowing.contains(carolId));
        assertFalse(aliceFollowing.contains(aliceId));
        assertEquals(List.of(carolId), followService.followingIds(bobId));
    }

    @Test
    void followingIds_whenNothingFollowed_isEmptyNotNull() {
        assertTrue(followService.followingIds(aliceId).isEmpty(),
                "无关注时应返回空列表，调用方据此展示空动态");
        assertTrue(followService.followingIds(null).isEmpty());
    }

    @Test
    void listFollowing_and_listFollowers_exposeDisplayNames() {
        followService.follow(aliceId, bobId);
        followService.follow(aliceId, carolId);

        Page<UserSummary> following = followService.listFollowing(aliceId, PageRequest.of(0, 10));
        assertEquals(2, following.getTotalElements());
        // 已绑定学生身份的用真实姓名展示，未绑定的退回用户名
        List<String> names = following.getContent().stream().map(UserSummary::displayName).toList();
        assertTrue(names.contains("张三"), "应展示绑定的学生姓名: " + names);
        assertTrue(names.contains("李四"), "应展示绑定的学生姓名: " + names);

        Page<UserSummary> followers = followService.listFollowers(bobId, PageRequest.of(0, 10));
        assertEquals(1, followers.getTotalElements());
        assertEquals("alice", followers.getContent().get(0).displayName(),
                "未绑定身份时退回用户名");
    }

    @Test
    void listFollowing_returnsEmptyPageForUnknownUser() {
        Page<UserSummary> page = followService.listFollowing(999999L, PageRequest.of(0, 10));

        assertEquals(0, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
    }

    @Test
    void isFollowing_handlesNullArgs() {
        assertFalse(followService.isFollowing(null, bobId));
        assertFalse(followService.isFollowing(aliceId, null));
    }

    @Test
    void stats_forNullUser_isZero() {
        FollowService.FollowStats stats = followService.stats(null);

        assertEquals(0, stats.following());
        assertEquals(0, stats.followers());
    }

    @Test
    void userSummary_carriesAvatarAndUserType() {
        User bob = users.findById(bobId).orElseThrow();
        bob.setAvatar("/api/media/7/content");
        bob.setUserType(User.UserType.STUDENT);
        users.save(bob);

        Page<UserSummary> following = followService.listFollowing(aliceId, PageRequest.of(0, 10));
        assertEquals(0, following.getTotalElements());

        followService.follow(aliceId, bobId);
        UserSummary summary = followService.listFollowing(aliceId, PageRequest.of(0, 10))
                .getContent().get(0);

        assertEquals("/api/media/7/content", summary.avatar());
        assertEquals("STUDENT", summary.userType());
        assertEquals(true, summary.followedByMe());
        assertEquals(bobId, summary.id());
        assertNull(summary.teacherId(), "未绑定教师时 teacherId 应为 null");
    }

    /** 建用户；(可选) 绑定一个学生身份用于验证展示名回退逻辑 */
    private Long createUser(String username, String studentName) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("x");
        if (studentName != null) {
            Student student = new Student();
            student.setName(studentName);
            student.setStudentNumber("S-" + username);
            user.setStudent(student);
            user.setUserType(User.UserType.STUDENT);
        }
        return users.save(user).getId();
    }
}
