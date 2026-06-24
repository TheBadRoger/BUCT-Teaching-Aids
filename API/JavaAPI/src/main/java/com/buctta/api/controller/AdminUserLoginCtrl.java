package com.buctta.api.controller;

import com.buctta.api.entities.AdminUser;
import com.buctta.api.service.AdminUserLoginService;
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
    private final AdminUserLoginService userLogin;

    @PostMapping("/login")
    public ApiResponse<AdminUser> loginCall(@RequestParam String username, @RequestParam String password) {
        AdminUserLoginService.LoginResult result = userLogin.login(username, password);
        if (result.success()) {
            return ApiResponse.ok(result.user());
        }
        else {
            return ApiResponse.fail(BusinessStatus.ACCOUNT_PASSWORD_ERROR, result.message());
        }
    }

    @PostMapping("/register")
    public ApiResponse<AdminUser> registerCall(@RequestBody AdminUser newUser) {
        AdminUserLoginService.RegisterResult result = userLogin.register(newUser);
        if (result.success()) {
            AdminUser adminUser = result.user();
            adminUser.setPassword("***************");
            return ApiResponse.ok(adminUser);
        }
        else {
            return ApiResponse.fail(BusinessStatus.USERNAME_EXISTS, result.message());
        }
    }
}
