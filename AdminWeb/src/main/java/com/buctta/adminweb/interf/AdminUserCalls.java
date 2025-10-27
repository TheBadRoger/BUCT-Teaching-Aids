package com.buctta.adminweb.interf;

import com.buctta.adminweb.entities.AdminUser;
import com.buctta.adminweb.service.UserLogin;
import com.buctta.adminweb.utils.CallBackContainer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/userlogin")

public class AdminUserCalls {
    @Resource
    private UserLogin userLogin;

    @PostMapping("/login")
    public CallBackContainer<AdminUser> loginCall(@RequestParam String username, @RequestParam String password){
        System.out.println("收到登录请求：\n"+
                "用户名："+username+"\n"+
                "\n正在处理登录请求...");

        AdminUser adminUser = userLogin.login(username, password);
        CallBackContainer<AdminUser> loginCallBack = new CallBackContainer<>();
        if(adminUser != null){
            loginCallBack.setData(adminUser);
            loginCallBack.setCode("0");
            loginCallBack.setMsg("登录成功");
            System.out.println("处理完成。返回消息：\""+loginCallBack.getMsg()+"\"("+loginCallBack.getCode()+")\n");
            return loginCallBack;
        }
        else{
            loginCallBack.setCode("-1");
            loginCallBack.setMsg("用户名或密码错误");
            System.out.println("处理完成。返回消息：\""+loginCallBack.getMsg()+"\"("+loginCallBack.getCode()+")\n");
            return loginCallBack;
        }
    }

    @PostMapping("/register")
    public CallBackContainer<AdminUser> registCall(@RequestBody AdminUser newUser){
        System.out.println("收到新用户注册请求：\n"+
                "用户名："+newUser.getUsername()+
                "\n正在处理注册请求...");

        AdminUser adminUser = userLogin.register(newUser);

        CallBackContainer<AdminUser> registerCallBack = new CallBackContainer<>();
        if(adminUser != null){
            registerCallBack.setData(adminUser);
            registerCallBack.setCode("0");
            registerCallBack.setMsg("注册成功");
            System.out.println("处理完成。返回消息：\""+registerCallBack.getMsg()+"\"("+registerCallBack.getCode()+")\n");

            return registerCallBack;
        }
        else{
            registerCallBack.setCode("-2");
            registerCallBack.setMsg("用户名已被注册");
            System.out.println("处理完成。返回消息：\""+registerCallBack.getMsg()+"\"("+registerCallBack.getCode()+")\n");

            return registerCallBack;
        }
    }
}
