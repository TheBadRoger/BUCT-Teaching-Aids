package com.buctta.api.serviceimp;

import com.buctta.api.dao.UserReposit;
import com.buctta.api.entities.User;
import com.buctta.api.service.UserAuthService;
import com.buctta.api.service.VerificationCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IMPL_UserAuthServiceTest {

    @Mock
    private UserReposit userReposit;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private IMPL_UserAuthService userAuthService;

    // ─── loginByPassword ────────────────────────────────────────────────────

    @Test
    void loginByPassword_validCredentials_returnsUser() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("hashed");

        when(userReposit.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain", "hashed")).thenReturn(true);

        User result = userAuthService.loginByPassword("alice", "plain");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getPassword()).isNull(); // password cleared
    }

    @Test
    void loginByPassword_wrongPassword_returnsNull() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("hashed");

        when(userReposit.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        User result = userAuthService.loginByPassword("alice", "wrong");

        assertThat(result).isNull();
    }

    @Test
    void loginByPassword_userNotFound_returnsNull() {
        when(userReposit.findByUsername("nobody")).thenReturn(Optional.empty());

        User result = userAuthService.loginByPassword("nobody", "pass");

        assertThat(result).isNull();
    }

    // ─── loginByTelephoneCode ────────────────────────────────────────────────

    @Test
    void loginByTelephoneCode_validCode_returnsUser() {
        User user = new User();
        user.setTelephone("13800138000");
        user.setPassword("hashed");

        when(verificationCodeService.verifySmsCode("13800138000", "123456"))
                .thenReturn(VerificationCodeService.VerifyResult.ok());
        when(userReposit.findByTelephone("13800138000")).thenReturn(Optional.of(user));

        User result = userAuthService.loginByTelephoneCode("13800138000", "123456");

        assertThat(result).isNotNull();
        assertThat(result.getPassword()).isNull();
    }

    @Test
    void loginByTelephoneCode_invalidCode_returnsNull() {
        when(verificationCodeService.verifySmsCode("13800138000", "wrong"))
                .thenReturn(VerificationCodeService.VerifyResult.fail("INVALID", "invalid code"));

        User result = userAuthService.loginByTelephoneCode("13800138000", "wrong");

        assertThat(result).isNull();
    }

    @Test
    void loginByTelephoneCode_userNotFound_returnsNull() {
        when(verificationCodeService.verifySmsCode("13800138000", "123456"))
                .thenReturn(VerificationCodeService.VerifyResult.ok());
        when(userReposit.findByTelephone("13800138000")).thenReturn(Optional.empty());

        User result = userAuthService.loginByTelephoneCode("13800138000", "123456");

        assertThat(result).isNull();
    }

    // ─── loginByEmailCode ────────────────────────────────────────────────────

    @Test
    void loginByEmailCode_validCode_returnsUser() {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setPassword("hashed");

        when(verificationCodeService.verifyEmailCode("alice@example.com", "654321"))
                .thenReturn(VerificationCodeService.VerifyResult.ok());
        when(userReposit.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        User result = userAuthService.loginByEmailCode("alice@example.com", "654321");

        assertThat(result).isNotNull();
        assertThat(result.getPassword()).isNull();
    }

    @Test
    void loginByEmailCode_invalidCode_returnsNull() {
        when(verificationCodeService.verifyEmailCode("alice@example.com", "wrong"))
                .thenReturn(VerificationCodeService.VerifyResult.fail("INVALID", "invalid code"));

        User result = userAuthService.loginByEmailCode("alice@example.com", "wrong");

        assertThat(result).isNull();
    }

    // ─── registerByTelephone ─────────────────────────────────────────────────

    @Test
    void registerByTelephone_success() {
        when(verificationCodeService.verifySmsCode("13900139000", "111111"))
                .thenReturn(VerificationCodeService.VerifyResult.ok());
        when(userReposit.existsByTelephone("13900139000")).thenReturn(false);
        when(userReposit.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("bob");
        savedUser.setTelephone("13900139000");
        savedUser.setPassword("encodedPass");
        when(userReposit.save(any(User.class))).thenReturn(savedUser);

        UserAuthService.RegisterResult result =
                userAuthService.registerByTelephone("13900139000", "111111", "bob", "pass");

        assertThat(result.success()).isTrue();
        assertThat(result.user()).isNotNull();
        assertThat(result.user().getPassword()).isNull(); // password cleared
    }

    @Test
    void registerByTelephone_invalidCode_returnsInvalidCode() {
        when(verificationCodeService.verifySmsCode("13900139000", "wrong"))
                .thenReturn(VerificationCodeService.VerifyResult.fail("INVALID", "invalid"));

        UserAuthService.RegisterResult result =
                userAuthService.registerByTelephone("13900139000", "wrong", "bob", "pass");

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALID_CODE");
    }

    @Test
    void registerByTelephone_phoneExists_returnsPhoneExists() {
        when(verificationCodeService.verifySmsCode("13900139000", "111111"))
                .thenReturn(VerificationCodeService.VerifyResult.ok());
        when(userReposit.existsByTelephone("13900139000")).thenReturn(true);

        UserAuthService.RegisterResult result =
                userAuthService.registerByTelephone("13900139000", "111111", "bob", "pass");

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("PHONE_EXISTS");
    }

    @Test
    void registerByTelephone_usernameExists_returnsUsernameExists() {
        when(verificationCodeService.verifySmsCode("13900139000", "111111"))
                .thenReturn(VerificationCodeService.VerifyResult.ok());
        when(userReposit.existsByTelephone("13900139000")).thenReturn(false);
        when(userReposit.existsByUsername("bob")).thenReturn(true);

        UserAuthService.RegisterResult result =
                userAuthService.registerByTelephone("13900139000", "111111", "bob", "pass");

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("USERNAME_EXISTS");
    }

    // ─── registerByEmail ─────────────────────────────────────────────────────

    @Test
    void registerByEmail_success() {
        when(verificationCodeService.verifyEmailCode("bob@example.com", "222222"))
                .thenReturn(VerificationCodeService.VerifyResult.ok());
        when(userReposit.existsByEmail("bob@example.com")).thenReturn(false);
        when(userReposit.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername("bob");
        savedUser.setEmail("bob@example.com");
        savedUser.setPassword("encodedPass");
        when(userReposit.save(any(User.class))).thenReturn(savedUser);

        UserAuthService.RegisterResult result =
                userAuthService.registerByEmail("bob@example.com", "222222", "bob", "pass");

        assertThat(result.success()).isTrue();
        assertThat(result.user().getPassword()).isNull();
    }

    @Test
    void registerByEmail_emailExists_returnsEmailExists() {
        when(verificationCodeService.verifyEmailCode("bob@example.com", "222222"))
                .thenReturn(VerificationCodeService.VerifyResult.ok());
        when(userReposit.existsByEmail("bob@example.com")).thenReturn(true);

        UserAuthService.RegisterResult result =
                userAuthService.registerByEmail("bob@example.com", "222222", "bob", "pass");

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("EMAIL_EXISTS");
    }
}
