package com.buctta.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
public class UserAuthServiceTest {
    @Autowired
    private UserAuthService userAuthService;

    @Test
    void testServiceNotNull() {
        assert(userAuthService != null);
    }
}
