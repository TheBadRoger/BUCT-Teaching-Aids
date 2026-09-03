package com.buctta.api.controller;

import com.buctta.api.entities.AdminUser;
import com.buctta.api.service.AdminUserLoginService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserLoginCtrl {
    @Resource
    private final AdminUserLoginService userLogin;

    @PostMapping("/login")
    public ApiResponse<AdminUser> loginCall(@RequestParam String username, @RequestParam String password,
                                            HttpServletRequest request, HttpServletResponse response) {
        AdminUserLoginService.LoginResult result = userLogin.login(username, password);
        if (result.success()) {
            AdminUser adminUser = result.user();

            // 手动写入认证信息，使其通过 Spring Security 的 anyRequest().authenticated() 校验
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(adminUser, null,
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN"))));

            // 持久化到 session，前端后续请求通过 Cookie 携带 JSESSIONID 通过校验
            new HttpSessionSecurityContextRepository()
                    .saveContext(SecurityContextHolder.getContext(), request, response);

            return ApiResponse.ok(adminUser);
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
