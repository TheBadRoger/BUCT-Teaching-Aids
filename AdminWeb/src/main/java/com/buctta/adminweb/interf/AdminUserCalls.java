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
    public CallBackContainer<AdminUser> loginCall(@RequestParam String name, @RequestParam String password){
        AdminUser adminUser = userLogin.login(name, password);
        if(adminUser != null){
            return CallBackContainer.Succeed((AdminUser) userLogin,"Login succeeded");
        }
        else{
            return CallBackContainer.Failed("-1","Login Failed");
        }
    }

    @PostMapping("/register")
    public CallBackContainer<AdminUser> registCall(@RequestBody AdminUser newUser){
        AdminUser adminUser = userLogin.register(newUser);
        if(adminUser != null){
            return CallBackContainer.Succeed(adminUser,"Register suceeded");
        }else{
            return CallBackContainer.Failed("-2","Username has been already registered");
        }
    }
}
