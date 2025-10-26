package com.buctta.adminweb.service;

import com.buctta.adminweb.entities.AdminUser;

public interface UserLogin {
    AdminUser login(String username, String password);
    AdminUser register(AdminUser RequestUser);
}
