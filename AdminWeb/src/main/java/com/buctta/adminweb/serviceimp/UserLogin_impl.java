package com.buctta.adminweb.serviceimp;

import com.buctta.adminweb.entities.AdminUser;
import com.buctta.adminweb.reposit.AdminQuery;
import com.buctta.adminweb.service.UserLogin;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service

public class UserLogin_impl implements UserLogin {

    @Resource
    private AdminQuery adminQuery;

    @Override
    public AdminUser login(String username, String password) {
        AdminUser adminUser = adminQuery.findAdminUserByUsernameAndPassword(username, password);
        if (adminUser != null) { adminUser.setPassword(null); }
        return adminUser;
    }

    @Override
    public AdminUser register(AdminUser RequestUser) {
        if(adminQuery.findAdminUserByUsername(RequestUser.getUsername()) != null) {
            return null;
        }
        else{
            AdminUser newUser = adminQuery.save(RequestUser);
            if(newUser != null) { newUser.setPassword(null); }
            return newUser;
        }
    }
}
