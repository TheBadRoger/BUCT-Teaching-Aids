package com.buctta.api.service;

import com.buctta.api.entities.JudgementUser;

/**
 * 评审用户登录服务接口
 */
public interface JudgeUserLoginService {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    LoginResult login(String username, String password);

    /**
     * 用户注册
     *
     * @param requestUser 注册用户信息
     * @return 注册结果
     */
    RegisterResult register(JudgementUser requestUser);

    /**
     * 登录结果
     */
    record LoginResult(boolean success, JudgementUser user, String errorCode, String message) {
        public static LoginResult success(JudgementUser user) {
            return new LoginResult(true, user, null, "登录成功");
        }

        public static LoginResult fail(String errorCode, String message) {
            return new LoginResult(false, null, errorCode, message);
        }
    }

    /**
     * 注册结果
     */
    record RegisterResult(boolean success, JudgementUser user, String errorCode, String message) {
        public static RegisterResult success(JudgementUser user) {
            return new RegisterResult(true, user, null, "注册成功");
        }

        public static RegisterResult fail(String errorCode, String message) {
            return new RegisterResult(false, null, errorCode, message);
        }
    }
}
