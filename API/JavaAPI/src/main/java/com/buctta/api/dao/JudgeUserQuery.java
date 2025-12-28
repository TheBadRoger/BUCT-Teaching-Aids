package com.buctta.api.dao;

import com.buctta.api.entities.JudgementUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface JudgeUserQuery extends JpaRepository<JudgementUser, Integer> {
    JudgementUser findJudgementUserByUsername(String username);
    JudgementUser findJudgementUserByUsernameAndPassword(String username, String password);
}