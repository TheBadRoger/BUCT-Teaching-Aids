package com.buctta.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CourseCallsTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testSearchCourse_Default() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/course/search", String.class);
        assert(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }
}
