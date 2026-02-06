package com.buctta.api.controller;

import com.buctta.api.entities.JudgementUser;
import com.buctta.api.service.JudgeUserLogin;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/aijudegment")
public class JudgeUserLoginCtrl {
    @Resource
    private JudgeUserLogin userLogin;

    @PostMapping("/login")
    public ApiResponse<JudgementUser> loginCall(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        JudgementUser judgementUser = userLogin.login(username, password);
        if (judgementUser != null) {

            //手动写入认证信息
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(judgementUser, null,
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_judgeUser"))));

            //持久化到 session
            new HttpSessionSecurityContextRepository()
                    .saveContext(SecurityContextHolder.getContext(), request, response);

            return ApiResponse.ok(judgementUser);
        }
        else
            return ApiResponse.fail(BusinessStatus.ACCOUNT_PASSWORD_ERROR);
    }

    @PostMapping("/register")
    public ApiResponse<JudgementUser> registerCall(@RequestBody JudgementUser newUser) {
        JudgementUser judgementUser = userLogin.register(newUser);
        if (judgementUser != null) {
            judgementUser.setPassword("****************");
            return ApiResponse.ok(judgementUser);
        }
        else
            return ApiResponse.fail(BusinessStatus.USERNAME_EXISTS);
    }
}