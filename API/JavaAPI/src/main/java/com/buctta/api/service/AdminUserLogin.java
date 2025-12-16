package com.buctta.api.service;

import com.buctta.api.entities.AdminUser;

public interface AdminUserLogin {
    AdminUser login(String username, String password);
    AdminUser register(AdminUser RequestUser);
}
