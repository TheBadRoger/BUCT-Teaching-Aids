package com.buctta.api.controller;

import com.buctta.api.entities.AdminUser;
import com.buctta.api.service.AdminUserLogin;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserLoginCtrl {
    @Resource
    private final AdminUserLogin userLogin;

    @PostMapping("/login")
    public ApiResponse<AdminUser> loginCall(@RequestParam String username, @RequestParam String password) {
        AdminUser adminUser = userLogin.login(username, password);
        if (adminUser != null)
            return ApiResponse.ok(adminUser);
        else {
            return ApiResponse.fail(BusinessStatus.ACCOUNT_PASSWORD_ERROR);
        }
    }

    @PostMapping("/register")
    public ApiResponse<AdminUser> registerCall(@RequestBody AdminUser newUser) {
        AdminUser adminUser = userLogin.register(newUser);
        if (adminUser != null) {
            adminUser.setPassword("***************");
            return ApiResponse.ok(adminUser);
        }
        else
            return ApiResponse.fail(BusinessStatus.USERNAME_EXISTS);
    }
}
