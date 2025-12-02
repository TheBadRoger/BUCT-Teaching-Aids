package com.buctta.api.interf;

import com.buctta.api.entities.AdminUser;
import com.buctta.api.service.AdminUserLogin;
import com.buctta.api.utils.CallBackContainer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")

public class AdminUserLoginCalls {
    @Resource
    private AdminUserLogin userLogin;

    @PostMapping("/login")
    public CallBackContainer<AdminUser> loginCall(@RequestParam String username, @RequestParam String password){
        AdminUser adminUser = userLogin.login(username, password);
        if(adminUser != null)
            return new CallBackContainer<AdminUser>("0","登陆成功",adminUser);
        else
            return new CallBackContainer<AdminUser>("-1","用户名或密码错误",adminUser);
    }

    @PostMapping("/register")
    public CallBackContainer<AdminUser> registerCall(@RequestBody AdminUser newUser){
        AdminUser adminUser = userLogin.register(newUser);
        CallBackContainer<AdminUser> registerCallBack = new CallBackContainer<>();
        if(adminUser != null)
            return new CallBackContainer<AdminUser>("0","注册成功",null);
        else
            return new CallBackContainer<AdminUser>("-2","用户名已被注册",null);
    }
}
