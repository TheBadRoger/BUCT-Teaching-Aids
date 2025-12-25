package com.buctta.api.interf;

import com.buctta.api.entities.AdminUser;
import com.buctta.api.service.AdminUserLogin;
import com.buctta.api.utils.ResponseContainer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aijudegment")

public class AdminUserLoginCalls {
    @Resource
    private AdminUserLogin userLogin;

    @PostMapping("/login")
    public ResponseContainer<AdminUser> loginCall(@RequestParam String username, @RequestParam String password){
        AdminUser adminUser = userLogin.login(username, password);
        if(adminUser != null)
            return new ResponseContainer<>("0","登陆成功",adminUser);
        else
            return new ResponseContainer<>("-1","用户名或密码错误", null);
    }

    @PostMapping("/register")
    public ResponseContainer<AdminUser> registerCall(@RequestBody AdminUser newUser){
        AdminUser adminUser = userLogin.register(newUser);
        ResponseContainer<AdminUser> registerCallBack = new ResponseContainer<>();
        if(adminUser != null)
            return new ResponseContainer<>("0","注册成功",adminUser);
        else
            return new ResponseContainer<>("-2","用户名已被注册",null);
    }
}
