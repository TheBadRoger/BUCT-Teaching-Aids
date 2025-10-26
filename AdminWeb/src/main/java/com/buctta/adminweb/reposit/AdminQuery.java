package com.buctta.adminweb.reposit;

import com.buctta.adminweb.entities.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface AdminQuery extends JpaRepository<AdminUser, Integer> {
    AdminUser findAdminUserByUsername(String username);
    AdminUser findAdminUserByUsernameAndPassword(String username, String password);
}
