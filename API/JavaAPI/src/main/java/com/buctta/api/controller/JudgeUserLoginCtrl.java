package com.buctta.api.controller;

import com.buctta.api.entities.JudgementUser;
import com.buctta.api.service.JudgeUserLogin;
import com.buctta.api.utils.ResponseContainer;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
    public ResponseContainer<JudgementUser> loginCall(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response
    ){
        JudgementUser judgementUser = userLogin.login(username, password);
        if(judgementUser != null) {

            //手动写入认证信息
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(judgementUser, null,
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_judgeUser"))));

            //持久化到 session
            new HttpSessionSecurityContextRepository()
                    .saveContext(SecurityContextHolder.getContext(), request, response);

            return new ResponseContainer<>(0, "登陆成功", judgementUser);
        }
        else
            return new ResponseContainer<>(1001,"用户名或密码错误",null);
    }

    @PostMapping("/register")
    public ResponseContainer<JudgementUser> registerCall(@RequestBody JudgementUser newUser){
        JudgementUser judgementUser = userLogin.register(newUser);
        ResponseContainer<JudgementUser> registerCallBack = new ResponseContainer<>();
        if(judgementUser != null)
            return new ResponseContainer<>(0,"注册成功",null);
        else
            return new ResponseContainer<>(1002,"用户名已被注册",null);
    }

    @GetMapping("/debug")
    public String debug(HttpSession session) {
        return "SessionId=" + session.getId() + "\n" +
                "Auth=" + SecurityContextHolder.getContext().getAuthentication();
    }
}