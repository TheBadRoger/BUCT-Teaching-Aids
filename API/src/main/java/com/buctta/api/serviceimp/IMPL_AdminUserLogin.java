package com.buctta.api.serviceimp;

import com.buctta.api.entities.AdminUser;
import com.buctta.api.reposit.AdminQuery;
import com.buctta.api.service.AdminUserLogin;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service

public class IMPL_AdminUserLogin implements AdminUserLogin {

    @Resource
    private AdminQuery adminQuery;

    @Override
    public AdminUser login(String username, String password) {
        AdminUser adminUser = adminQuery.findAdminUserByUsernameAndPassword(username, password);
        if (adminUser != null)
            adminUser.setPassword(null);
        return adminUser;
    }

    @Override
    public AdminUser register(AdminUser RequestUser) {
        if(adminQuery.findAdminUserByUsername(RequestUser.getUsername()) != null)
            return null;
        else{
            AdminUser newUser = adminQuery.save(RequestUser);
            newUser.setPassword(null);
            return newUser;
        }
    }
}
