package com.buctta.api.serviceimp;

import com.buctta.api.dao.JudgeUserQuery;
import com.buctta.api.entities.JudgementUser;
import com.buctta.api.service.JudgeUserLogin;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMPL_JudgeUserLogin implements JudgeUserLogin {
    @Resource
    private JudgeUserQuery judgeUserQuery;

    @Override
    public JudgementUser login(String username, String password) {
        JudgementUser JudgementUser = judgeUserQuery.findJudgementUserByUsernameAndPassword(username, password);
        if (JudgementUser != null)
            JudgementUser.setPassword(null);
        return JudgementUser;
    }

    @Override
    public JudgementUser register(JudgementUser RequestUser) {
        if(judgeUserQuery.findJudgementUserByUsername(RequestUser.getUsername()) != null)
            return null;
        else{
            JudgementUser newUser = judgeUserQuery.save(RequestUser);
            newUser.setPassword(null);
            return newUser;
        }
    }
}
