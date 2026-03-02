package com.buctta.api.serviceimp;

import com.buctta.api.dao.AdminReposit;
import com.buctta.api.entities.AdminUser;
import com.buctta.api.service.AdminUserLoginService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMPL_AdminUserLogin implements AdminUserLoginService {
    @Resource
    private AdminReposit adminQuery;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public LoginResult login(String username, String password) {
        // 先根据用户名查找用户
        AdminUser adminUser = adminQuery.findAdminUserByUsername(username);
        if (adminUser != null) {
            // 使用 BCrypt 验证密码（慢哈希+加盐）
            if (passwordEncoder.matches(password, adminUser.getPassword())) {
                adminUser.setPassword(null);
                return LoginResult.success(adminUser);
            }
        }
        return LoginResult.fail("INVALID_CREDENTIALS", "用户名或密码错误");
    }

    @Override
    public RegisterResult register(AdminUser requestUser) {
        if (adminQuery.findAdminUserByUsername(requestUser.getUsername()) != null) {
            return RegisterResult.fail("USERNAME_EXISTS", "用户名已存在");
        }
        try {
            // 使用 BCrypt 加密密码（慢哈希+加盐）
            String encodedPassword = passwordEncoder.encode(requestUser.getPassword());
            requestUser.setPassword(encodedPassword);

            AdminUser newUser = adminQuery.save(requestUser);
            newUser.setPassword(null);
            return RegisterResult.success(newUser);
        }
        catch (Exception e) {
            return RegisterResult.fail("REGISTER_FAILED", "注册失败: " + e.getMessage());
        }
    }
}
