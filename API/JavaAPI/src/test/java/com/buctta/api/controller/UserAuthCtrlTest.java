package com.buctta.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserAuthCtrlTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testLogin_BadRequest() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/user/auth/login", null, String.class);
        assert(response.getStatusCode().is4xxClientError());
    }

    @Test
    void testCurrentUser_Unauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/user/auth/current", String.class);
        assert(response.getStatusCode().is4xxClientError());
    }
}
