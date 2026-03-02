package com.buctta.api.dao;

import com.buctta.api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserReposit extends JpaRepository<User, Long> {

    // 根据用户名查找用户
    Optional<User> findByUsername(String username);

    // 根据邮箱查找用户
    Optional<User> findByEmail(String email);

    // 根据电话查找用户
    Optional<User> findByTelephone(String telephone);

    // 检查用户名是否存在
    boolean existsByUsername(String username);

    // 检查邮箱是否存在
    boolean existsByEmail(String email);

    // 检查电话是否存在
    boolean existsByTelephone(String telephone);

    // 根据 Teacher ID 查找用户
    Optional<User> findByTeacherId(Long teacherId);

    // 根据 Student ID 查找用户
    Optional<User> findByStudentId(Long studentId);
}

