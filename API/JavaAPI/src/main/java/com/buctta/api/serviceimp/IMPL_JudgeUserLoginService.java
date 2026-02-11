package com.buctta.api.serviceimp;

import com.buctta.api.dao.JudgeUserQuery;
import com.buctta.api.entities.JudgementUser;
import com.buctta.api.service.JudgeUserLoginService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMPL_JudgeUserLoginService implements JudgeUserLoginService {
    @Resource
    private JudgeUserQuery judgeUserQuery;

    @Override
    public LoginResult login(String username, String password) {
        JudgementUser judgementUser;
        if (password == null) {
            judgementUser = judgeUserQuery.findJudgementUserByUsername(username);
        }
        else {
            judgementUser = judgeUserQuery.findJudgementUserByUsernameAndPassword(username, password);
        }

        if (judgementUser != null) {
            judgementUser.setPassword(null);
            return LoginResult.success(judgementUser);
        }
        return LoginResult.fail("INVALID_CREDENTIALS", "用户名或密码错误");
    }

    @Override
    public RegisterResult register(JudgementUser requestUser) {
        if (judgeUserQuery.findJudgementUserByUsername(requestUser.getUsername()) != null) {
            return RegisterResult.fail("USERNAME_EXISTS", "用户名已存在");
        }
        try {
            JudgementUser newUser = judgeUserQuery.save(requestUser);
            newUser.setPassword(null);
            return RegisterResult.success(newUser);
        }
        catch (Exception e) {
            return RegisterResult.fail("REGISTER_FAILED", "注册失败: " + e.getMessage());
        }
    }
}
