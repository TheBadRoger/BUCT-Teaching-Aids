package com.buctta.api.dao;

import com.buctta.api.entities.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    Optional<UserFollow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    /** 我关注的人 */
    @Query("SELECT f FROM UserFollow f WHERE f.follower.id = :followerId")
    Page<UserFollow> findByFollowerId(@Param("followerId") Long followerId, Pageable pageable);

    /** 关注我的人 */
    @Query("SELECT f FROM UserFollow f WHERE f.followee.id = :followeeId")
    Page<UserFollow> findByFolloweeId(@Param("followeeId") Long followeeId, Pageable pageable);

    long countByFollowerId(Long followerId);

    long countByFolloweeId(Long followeeId);

    /** 我关注的人的 ID 列表，用于筛选动态流 */
    @Query("SELECT f.followee.id FROM UserFollow f WHERE f.follower.id = :followerId")
    List<Long> findFolloweeIds(@Param("followerId") Long followerId);
}
