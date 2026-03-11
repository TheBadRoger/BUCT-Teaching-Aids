package com.buctta.api.serviceimp;

import com.buctta.api.dao.AdminReposit;
import com.buctta.api.entities.AdminUser;
import com.buctta.api.service.AdminUserLoginService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IMPL_AdminUserLoginTest {

    @Mock
    private AdminReposit adminQuery;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private IMPL_AdminUserLogin adminUserLogin;

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsSuccess() {
        AdminUser adminUser = new AdminUser(1L, "admin", "hashed");
        when(adminQuery.findAdminUserByUsername("admin")).thenReturn(adminUser);
        when(passwordEncoder.matches("plain", "hashed")).thenReturn(true);

        AdminUserLoginService.LoginResult result = adminUserLogin.login("admin", "plain");

        assertThat(result.success()).isTrue();
        assertThat(result.user()).isNotNull();
        assertThat(result.user().getPassword()).isNull(); // password cleared
    }

    @Test
    void login_wrongPassword_returnsFail() {
        AdminUser adminUser = new AdminUser(1L, "admin", "hashed");
        when(adminQuery.findAdminUserByUsername("admin")).thenReturn(adminUser);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        AdminUserLoginService.LoginResult result = adminUserLogin.login("admin", "wrong");

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void login_userNotFound_returnsFail() {
        when(adminQuery.findAdminUserByUsername("unknown")).thenReturn(null);

        AdminUserLoginService.LoginResult result = adminUserLogin.login("unknown", "pass");

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALID_CREDENTIALS");
    }

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    void register_newUser_returnsSuccess() {
        AdminUser requestUser = new AdminUser(0L, "newadmin", "plainPass");
        when(adminQuery.findAdminUserByUsername("newadmin")).thenReturn(null);
        when(passwordEncoder.encode("plainPass")).thenReturn("encodedPass");

        AdminUser savedUser = new AdminUser(2L, "newadmin", null);
        when(adminQuery.save(any(AdminUser.class))).thenReturn(savedUser);

        AdminUserLoginService.RegisterResult result = adminUserLogin.register(requestUser);

        assertThat(result.success()).isTrue();
        assertThat(result.user()).isNotNull();
    }

    @Test
    void register_existingUsername_returnsFail() {
        AdminUser existingUser = new AdminUser(1L, "admin", "hashed");
        AdminUser requestUser = new AdminUser(0L, "admin", "plainPass");
        when(adminQuery.findAdminUserByUsername("admin")).thenReturn(existingUser);

        AdminUserLoginService.RegisterResult result = adminUserLogin.register(requestUser);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("USERNAME_EXISTS");
    }
}
