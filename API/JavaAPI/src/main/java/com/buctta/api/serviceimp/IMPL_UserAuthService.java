package com.buctta.api.serviceimp;

import com.buctta.api.dao.UserReposit;
import com.buctta.api.entities.User;
import com.buctta.api.service.UserAuthService;
import com.buctta.api.service.VerificationCodeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class IMPL_UserAuthService implements UserAuthService {

    @Resource
    private UserReposit userReposit;

    @Resource
    private VerificationCodeService verificationCodeService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public User loginByPassword(String username, String password) {
        Optional<User> userOpt = userReposit.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                user.setPassword(null); // 不返回密码
                return user;
            }
        }
        return null;
    }

    @Override
    public User loginByTelephoneCode(String telephone, String code) {
        // 验证验证码
        VerificationCodeService.VerifyResult verifyResult = verificationCodeService.verifySmsCode(telephone, code);
        if (!verifyResult.success()) {
            return null;
        }

        Optional<User> userOpt = userReposit.findByTelephone(telephone);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(null); // 不返回密码
            return user;
        }
        return null;
    }

    @Override
    public User loginByEmailCode(String email, String code) {
        // 验证验证码
        VerificationCodeService.VerifyResult verifyResult = verificationCodeService.verifyEmailCode(email, code);
        if (!verifyResult.success()) {
            return null;
        }

        Optional<User> userOpt = userReposit.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(null); // 不返回密码
            return user;
        }
        return null;
    }

    @Override
    @Transactional
    public RegisterResult registerByTelephone(String telephone, String code, String username, String password) {
        // 验证验证码
        VerificationCodeService.VerifyResult verifyResult = verificationCodeService.verifySmsCode(telephone, code);
        if (!verifyResult.success()) {
            return RegisterResult.fail("INVALID_CODE");
        }

        // 检查手机号是否已存在
        if (userReposit.existsByTelephone(telephone)) {
            return RegisterResult.fail("PHONE_EXISTS");
        }

        // 检查用户名是否已存在
        if (userReposit.existsByUsername(username)) {
            return RegisterResult.fail("USERNAME_EXISTS");
        }

        // 创建新用户（不设置userType，绑定身份时自动设置）
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setTelephone(telephone);
        newUser.setPassword(passwordEncoder.encode(password));
        // userType 在绑定身份时自动设置

        User savedUser = userReposit.save(newUser);
        savedUser.setPassword(null); // 不返回密码

        return RegisterResult.success(savedUser);
    }

    @Override
    @Transactional
    public RegisterResult registerByEmail(String email, String code, String username, String password) {
        // 验证验证码
        VerificationCodeService.VerifyResult verifyResult = verificationCodeService.verifyEmailCode(email, code);
        if (!verifyResult.success()) {
            return RegisterResult.fail("INVALID_CODE");
        }

        // 检查邮箱是否已存在
        if (userReposit.existsByEmail(email)) {
            return RegisterResult.fail("EMAIL_EXISTS");
        }

        // 检查用户名是否已存在
        if (userReposit.existsByUsername(username)) {
            return RegisterResult.fail("USERNAME_EXISTS");
        }

        // 创建新用户（不设置userType，绑定身份时自动设置）
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        // userType 在绑定身份时自动设置

        User savedUser = userReposit.save(newUser);
        savedUser.setPassword(null); // 不返回密码

        return RegisterResult.success(savedUser);
    }
}

