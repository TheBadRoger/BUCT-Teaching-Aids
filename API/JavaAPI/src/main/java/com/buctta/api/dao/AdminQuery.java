package com.buctta.api.dao;

import com.buctta.api.entities.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface AdminQuery extends JpaRepository<AdminUser, Integer> {
    AdminUser findAdminUserByUsername(String username);
    AdminUser findAdminUserByUsernameAndPassword(String username, String password);
}