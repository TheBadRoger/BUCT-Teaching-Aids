package com.buctta.api.controller;

import com.buctta.api.dto.LoginRequest;
import com.buctta.api.dto.RegisterRequest;
import com.buctta.api.dto.SendCodeRequest;
import com.buctta.api.entities.User;
import com.buctta.api.service.UserAuthService;
import com.buctta.api.service.VerificationCodeService;
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

/**
 * 用户认证控制器
 * 提供登录、注册、发送验证码等接口
 */
@RestController
@RequestMapping("/api/user/auth")
public class UserAuthCtrl {

    @Resource
    private UserAuthService userAuthService;

    @Resource
    private VerificationCodeService verificationCodeService;

    /**
     * 统一登录接口
     * 支持三种登录方式：用户名密码、手机验证码、邮箱验证码
     */
    @PostMapping("/login")
    public ApiResponse<User> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        User user;

        switch (request.getLoginType()) {
            case PASSWORD:
                if (request.getUsername() == null || request.getPassword() == null) {
                    return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "username, password");
                }
                user = userAuthService.loginByPassword(request.getUsername(), request.getPassword());
                break;

            case SMS_CODE:
                if (request.getTelephone() == null || request.getCode() == null) {
                    return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "telephone, code");
                }
                user = userAuthService.loginByTelephoneCode(request.getTelephone(), request.getCode());
                break;

            case EMAIL_CODE:
                if (request.getEmail() == null || request.getCode() == null) {
                    return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "email, code");
                }
                user = userAuthService.loginByEmailCode(request.getEmail(), request.getCode());
                break;

            default:
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
        }

        if (user != null) {
            // 确定用户角色
            String role = user.getUserType() == User.UserType.TEACHER ? "ROLE_TEACHER" : "ROLE_STUDENT";

            // 手动写入认证信息
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null,
                            Collections.singleton(new SimpleGrantedAuthority(role))));

            // 持久化到 session
            new HttpSessionSecurityContextRepository()
                    .saveContext(SecurityContextHolder.getContext(), httpRequest, httpResponse);

            return ApiResponse.ok(user);
        }
        else {
            return ApiResponse.fail(BusinessStatus.ACCOUNT_PASSWORD_ERROR);
        }
    }

    /**
     * 统一注册接口
     * 支持两种注册方式：手机验证码、邮箱验证码
     * 注册时不需要指定用户类型，绑定身份时自动设置
     */
    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody RegisterRequest request) {
        // 参数校验
        if (request.getUsername() == null || request.getPassword() == null) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "username, password");
        }

        UserAuthService.RegisterResult result;

        switch (request.getRegisterType()) {
            case SMS_CODE:
                if (request.getTelephone() == null || request.getCode() == null) {
                    return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "telephone, code");
                }
                result = userAuthService.registerByTelephone(
                        request.getTelephone(),
                        request.getCode(),
                        request.getUsername(),
                        request.getPassword()
                );
                break;

            case EMAIL_CODE:
                if (request.getEmail() == null || request.getCode() == null) {
                    return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "email, code");
                }
                result = userAuthService.registerByEmail(
                        request.getEmail(),
                        request.getCode(),
                        request.getUsername(),
                        request.getPassword()
                );
                break;

            default:
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
        }

        if (result.success()) {
            return ApiResponse.ok(result.user());
        }
        else {
            // 根据错误码返回不同的错误信息
            return switch (result.errorCode()) {
                case "INVALID_CODE" -> ApiResponse.fail(BusinessStatus.VERIFICATION_CODE_ERROR);
                case "PHONE_EXISTS" -> ApiResponse.fail(BusinessStatus.PHONE_EXISTS);
                case "EMAIL_EXISTS" -> ApiResponse.fail(BusinessStatus.EMAIL_EXISTS);
                case "USERNAME_EXISTS" -> ApiResponse.fail(BusinessStatus.USERNAME_EXISTS);
                default -> ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
            };
        }
    }

    /**
     * 发送验证码接口
     */
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@RequestBody SendCodeRequest request) {
        VerificationCodeService.SendResult result;

        switch (request.getSendType()) {
            case SMS:
                if (request.getTelephone() == null) {
                    return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "telephone");
                }
                result = verificationCodeService.sendSmsCode(request.getTelephone());
                break;

            case EMAIL:
                if (request.getEmail() == null) {
                    return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "email");
                }
                result = verificationCodeService.sendEmailCode(request.getEmail());
                break;

            default:
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
        }

        if (result.success()) {
            return ApiResponse.ok();
        }
        else {
            return ApiResponse.fail(BusinessStatus.SEND_CODE_FAILED, result.message());
        }
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return ApiResponse.ok();
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public ApiResponse<User> getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return ApiResponse.ok(user);
        }
        return ApiResponse.fail(BusinessStatus.TOKEN_INVALID);
    }
}

