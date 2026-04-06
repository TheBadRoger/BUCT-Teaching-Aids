package com.buctta.api.controller;

import com.buctta.api.entities.JudgementUser;
import com.buctta.api.service.JudgeUserLoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JudgeUserLoginCtrlTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private JudgeUserLoginService userLogin;

    @InjectMocks
    private JudgeUserLoginCtrl judgeUserLoginCtrl;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(judgeUserLoginCtrl).build();
    }

    @Test
    void login_validCredentials_returns200() throws Exception {
        JudgementUser user = new JudgementUser(1L, "judge", "hidden");
        when(userLogin.login("judge", "pass"))
                .thenReturn(JudgeUserLoginService.LoginResult.success(user));

        mockMvc.perform(post("/api/aijudegment/login")
                        .param("username", "judge")
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.username").value("judge"));
    }

    @Test
    void login_invalidCredentials_returns4011() throws Exception {
        when(userLogin.login("judge", "bad"))
                .thenReturn(JudgeUserLoginService.LoginResult.fail("INVALID_CREDENTIALS", "bad credentials"));

        mockMvc.perform(post("/api/aijudegment/login")
                        .param("username", "judge")
                        .param("password", "bad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4011));
    }

    @Test
    void register_newUser_masksPassword() throws Exception {
        JudgementUser user = new JudgementUser(2L, "newjudge", "hashed");
        when(userLogin.register(any(JudgementUser.class)))
                .thenReturn(JudgeUserLoginService.RegisterResult.success(user));

        JudgementUser request = new JudgementUser(0L, "newjudge", "plainPass");

        mockMvc.perform(post("/api/aijudegment/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.password").value("****************"));
    }

    @Test
    void register_existingUsername_returns4092() throws Exception {
        when(userLogin.register(any(JudgementUser.class)))
                .thenReturn(JudgeUserLoginService.RegisterResult.fail("USERNAME_EXISTS", "username exists"));

        JudgementUser request = new JudgementUser(0L, "judge", "plainPass");

        mockMvc.perform(post("/api/aijudegment/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4092));
    }
}

