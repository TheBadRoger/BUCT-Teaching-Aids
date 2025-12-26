package com.buctta.api.interf;

import com.buctta.api.entities.JudgementUser;
import com.buctta.api.service.JudgeUserLogin;
import com.buctta.api.utils.ResponseContainer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.deser.jdk.JavaUtilCalendarDeserializer;

@RestController
@RequestMapping("/api/aijudegment")

public class JudgeUserLoginCalls {
    @Resource
    private JudgeUserLogin userLogin;

    @PostMapping("/login")
    public ResponseContainer<JudgementUser> loginCall(@RequestParam String username, @RequestParam String password){
        JudgementUser judgementUser = userLogin.login(username, password);
        if(judgementUser != null)
            return new ResponseContainer<>("0","登陆成功",judgementUser);
        else
            return new ResponseContainer<>("-1","用户名或密码错误",null);
    }

    @PostMapping("/register")
    public ResponseContainer<JudgementUser> registerCall(@RequestBody JudgementUser newUser){
        JudgementUser judgementUser = userLogin.register(newUser);
        ResponseContainer<JudgementUser> registerCallBack = new ResponseContainer<>();
        if(judgementUser != null)
            return new ResponseContainer<>("0","注册成功",null);
        else
            return new ResponseContainer<>("-2","用户名已被注册",null);
    }
}
