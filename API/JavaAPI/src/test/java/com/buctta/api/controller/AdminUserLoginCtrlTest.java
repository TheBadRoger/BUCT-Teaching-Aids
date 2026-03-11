package com.buctta.api.controller;

import com.buctta.api.entities.AdminUser;
import com.buctta.api.service.AdminUserLoginService;
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
class AdminUserLoginCtrlTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminUserLoginService userLogin;

    @InjectMocks
    private AdminUserLoginCtrl adminUserLoginCtrl;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserLoginCtrl).build();
    }

    // ─── /login ──────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200() throws Exception {
        AdminUser adminUser = new AdminUser(1L, "admin", null);
        when(userLogin.login("admin", "pass"))
                .thenReturn(AdminUserLoginService.LoginResult.success(adminUser));

        mockMvc.perform(post("/api/admin/login")
                        .param("username", "admin")
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void login_invalidCredentials_returns4011() throws Exception {
        when(userLogin.login("admin", "wrong"))
                .thenReturn(AdminUserLoginService.LoginResult.fail("INVALID_CREDENTIALS", "用户名或密码错误"));

        mockMvc.perform(post("/api/admin/login")
                        .param("username", "admin")
                        .param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4011));
    }

    // ─── /register ───────────────────────────────────────────────────────────

    @Test
    void register_newUser_returns200WithMaskedPassword() throws Exception {
        AdminUser savedUser = new AdminUser(2L, "newadmin", null);
        when(userLogin.register(any(AdminUser.class)))
                .thenReturn(AdminUserLoginService.RegisterResult.success(savedUser));

        AdminUser requestUser = new AdminUser(0L, "newadmin", "plainPass");

        mockMvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.username").value("newadmin"));
    }

    @Test
    void register_existingUsername_returns4092() throws Exception {
        when(userLogin.register(any(AdminUser.class)))
                .thenReturn(AdminUserLoginService.RegisterResult.fail("USERNAME_EXISTS", "用户名已存在"));

        AdminUser requestUser = new AdminUser(0L, "admin", "pass");

        mockMvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4092));
    }
}
