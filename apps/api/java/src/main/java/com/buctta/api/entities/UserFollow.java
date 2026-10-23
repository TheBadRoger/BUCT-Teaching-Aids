package com.buctta.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 关注关系（follower 关注 followee）。
 * <p>
 * 每个方向一行记录，(follower_id, followee_id) 唯一，重复关注由唯一约束兜底。
 * 该表是「好友动态」的基础：动态流可据此只展示我关注的人产生的内容。
 */
@Entity
@Table(name = "user_follow",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_follow_pair",
                columnNames = {"follower_id", "followee_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关注发起方 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    /** 被关注方 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followee;

    @Column(name = "created_time", insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdTime;

    public UserFollow(User follower, User followee) {
        this.follower = follower;
        this.followee = followee;
    }
}
