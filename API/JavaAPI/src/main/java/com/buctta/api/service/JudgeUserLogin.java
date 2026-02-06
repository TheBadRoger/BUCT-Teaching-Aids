package com.buctta.api.service;

import com.buctta.api.entities.JudgementUser;

public interface JudgeUserLogin {
    JudgementUser login(String username, String password);

    JudgementUser register(JudgementUser RequestUser);
}
