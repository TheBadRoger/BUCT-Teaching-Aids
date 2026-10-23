package com.buctta.api.controller;

import com.buctta.api.dto.LoginRequest;
import com.buctta.api.dto.RegisterRequest;
import com.buctta.api.dto.SendCodeRequest;
import com.buctta.api.entities.User;
import com.buctta.api.service.UserAuthService;
import com.buctta.api.service.VerificationCodeService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserAuthCtrlTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserAuthService userAuthService;

    @Mock
    private VerificationCodeService verificationCodeService;

    @InjectMocks
    private UserAuthCtrl userAuthCtrl;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userAuthCtrl).build();
    }

    // ─── /login ──────────────────────────────────────────────────────────────

    @Test
    void login_validPasswordCredentials_returns200WithUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        when(userAuthService.loginByPassword("alice", "pass")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginRequest.LoginType.PASSWORD);
        request.setUsername("alice");
        request.setPassword("pass");

        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void login_invalidPassword_returns4011() throws Exception {
        when(userAuthService.loginByPassword("alice", "wrong")).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginRequest.LoginType.PASSWORD);
        request.setUsername("alice");
        request.setPassword("wrong");

        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4011));
    }

    @Test
    void login_passwordType_missingUsername_returns4001() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginRequest.LoginType.PASSWORD);
        // username and password are null

        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void login_smsCodeType_validCode_returns200() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setTelephone("13800138000");
        when(userAuthService.loginByTelephoneCode("13800138000", "123456")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginRequest.LoginType.SMS_CODE);
        request.setTelephone("13800138000");
        request.setCode("123456");

        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void login_emailCodeType_missingEmail_returns4001() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginRequest.LoginType.EMAIL_CODE);
        // email and code are null

        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    // ─── /register ───────────────────────────────────────────────────────────

    @Test
    void register_smsCode_success_returns200() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");
        UserAuthService.RegisterResult result = UserAuthService.RegisterResult.success(user);
        when(userAuthService.registerByTelephone("13900139000", "111111", "bob", "pass"))
                .thenReturn(result);

        RegisterRequest request = new RegisterRequest();
        request.setRegisterType(RegisterRequest.RegisterType.SMS_CODE);
        request.setUsername("bob");
        request.setPassword("pass");
        request.setTelephone("13900139000");
        request.setCode("111111");

        mockMvc.perform(post("/api/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.username").value("bob"));
    }

    @Test
    void register_missingUsername_returns4001() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setRegisterType(RegisterRequest.RegisterType.SMS_CODE);
        // username is null

        mockMvc.perform(post("/api/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void register_invalidCode_returns4014() throws Exception {
        UserAuthService.RegisterResult result = UserAuthService.RegisterResult.fail("INVALID_CODE");
        when(userAuthService.registerByTelephone("13900139000", "wrong", "bob", "pass"))
                .thenReturn(result);

        RegisterRequest request = new RegisterRequest();
        request.setRegisterType(RegisterRequest.RegisterType.SMS_CODE);
        request.setUsername("bob");
        request.setPassword("pass");
        request.setTelephone("13900139000");
        request.setCode("wrong");

        mockMvc.perform(post("/api/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4014));
    }

    @Test
    void register_phoneExists_returns4094() throws Exception {
        UserAuthService.RegisterResult result = UserAuthService.RegisterResult.fail("PHONE_EXISTS");
        when(userAuthService.registerByTelephone("13900139000", "111111", "bob", "pass"))
                .thenReturn(result);

        RegisterRequest request = new RegisterRequest();
        request.setRegisterType(RegisterRequest.RegisterType.SMS_CODE);
        request.setUsername("bob");
        request.setPassword("pass");
        request.setTelephone("13900139000");
        request.setCode("111111");

        mockMvc.perform(post("/api/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4094));
    }

    // ─── /send-code ───────────────────────────────────────────────────────────

    @Test
    void sendCode_sms_success_returns200() throws Exception {
        when(verificationCodeService.sendSmsCode("13800138000"))
                .thenReturn(VerificationCodeService.SendResult.ok());

        SendCodeRequest request = new SendCodeRequest();
        request.setSendType(SendCodeRequest.SendType.SMS);
        request.setTelephone("13800138000");

        mockMvc.perform(post("/api/user/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void sendCode_email_success_returns200() throws Exception {
        when(verificationCodeService.sendEmailCode("alice@example.com"))
                .thenReturn(VerificationCodeService.SendResult.ok());

        SendCodeRequest request = new SendCodeRequest();
        request.setSendType(SendCodeRequest.SendType.EMAIL);
        request.setEmail("alice@example.com");

        mockMvc.perform(post("/api/user/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void sendCode_sms_missingTelephone_returns4001() throws Exception {
        SendCodeRequest request = new SendCodeRequest();
        request.setSendType(SendCodeRequest.SendType.SMS);
        // telephone is null

        mockMvc.perform(post("/api/user/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void sendCode_sendFailed_returns4015() throws Exception {
        when(verificationCodeService.sendSmsCode("13800138000"))
                .thenReturn(VerificationCodeService.SendResult.fail("FAIL", "send failed"));

        SendCodeRequest request = new SendCodeRequest();
        request.setSendType(SendCodeRequest.SendType.SMS);
        request.setTelephone("13800138000");

        mockMvc.perform(post("/api/user/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4015));
    }
}
