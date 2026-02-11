package com.buctta.api.service;

import com.buctta.api.entities.User;

/**
 * 用户认证服务接口
 */
public interface UserAuthService {

    /**
     * 用户名密码登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户信息，登录失败返回null
     */
    User loginByPassword(String username, String password);

    /**
     * 手机号验证码登录
     *
     * @param telephone 手机号
     * @param code      验证码
     * @return 用户信息，登录失败返回null
     */
    User loginByTelephoneCode(String telephone, String code);

    /**
     * 邮箱验证码登录
     *
     * @param email 邮箱
     * @param code  验证码
     * @return 用户信息，登录失败返回null
     */
    User loginByEmailCode(String email, String code);

    /**
     * 手机号验证码注册
     *
     * @param telephone 手机号
     * @param code      验证码
     * @param username  用户名
     * @param password  密码
     * @return 注册结果
     */
    RegisterResult registerByTelephone(String telephone, String code, String username, String password);

    /**
     * 邮箱验证码注册
     *
     * @param email    邮箱
     * @param code     验证码
     * @param username 用户名
     * @param password 密码
     * @return 注册结果
     */
    RegisterResult registerByEmail(String email, String code, String username, String password);

    /**
     * 注册结果
     */
    record RegisterResult(boolean success, User user, String errorCode) {
        public static RegisterResult success(User user) {
            return new RegisterResult(true, user, null);
        }

        public static RegisterResult fail(String errorCode) {
            return new RegisterResult(false, null, errorCode);
        }
    }
}

