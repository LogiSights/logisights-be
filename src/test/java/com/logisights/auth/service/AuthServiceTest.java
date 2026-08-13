package com.logisights.auth.service;

import com.logisights.auth.dto.*;
import com.logisights.auth.entity.EmailVerificationTokenEntity;
import com.logisights.auth.entity.PasswordResetTokenEntity;
import com.logisights.auth.entity.UserEntity;
import com.logisights.auth.repository.EmailVerificationTokenRepository;
import com.logisights.auth.repository.PasswordResetTokenRepository;
import com.logisights.auth.repository.UserRepository;
import com.logisights.common.ApiException;
import com.logisights.common.UserRole;
import com.logisights.common.UserStatus;
import com.logisights.notification.MailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    EmailVerificationTokenRepository verificationTokenRepository;
    @Mock
    PasswordResetTokenRepository resetTokenRepository;
    @Mock
    PasswordService passwordService;
    @Mock
    JwtService jwtService;
    @Mock
    MailSender mailSender;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        authService.userRepository = userRepository;
        authService.verificationTokenRepository = verificationTokenRepository;
        authService.resetTokenRepository = resetTokenRepository;
        authService.passwordService = passwordService;
        authService.jwtService = jwtService;
        authService.tokenGenerator = new TokenGenerator();
        authService.mailSender = mailSender;
        authService.frontendBaseUrl = "http://localhost:3000";
        authService.adminAlertEmail = "ops@logisights.co.ke";
    }

    @Test
    void registerRejectsAdminRole() {
        RegisterRequest request = new RegisterRequest("Evil", "evil@example.com", "254700000000",
                "password123", UserRole.ADMIN);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(ApiException.class);

        verifyNoInteractions(userRepository, mailSender);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Jane", "jane@example.com", "254700000000",
                "password123", UserRole.SENDER);
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(ApiException.class);
    }

    @Test
    void registerPersistsUserAndSendsVerificationAndAdminAlert() {
        RegisterRequest request = new RegisterRequest("Jane", "jane@example.com", "254700000000",
                "password123", UserRole.SENDER);
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordService.hash("password123")).thenReturn("hashed");

        UserDto result = authService.register(request);

        assertThat(result.name()).isEqualTo("Jane");
        assertThat(result.role()).isEqualTo(UserRole.SENDER);
        verify(userRepository).persist(any(UserEntity.class));
        verify(mailSender).sendVerificationEmail(eq("jane@example.com"), eq("Jane"), anyString());
        verify(mailSender).sendAdminNewUserAlert(eq("ops@logisights.co.ke"), eq("Jane"), eq("jane@example.com"), eq("SENDER"));
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "x")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        UserEntity user = new UserEntity();
        user.email = "jane@example.com";
        user.passwordHash = "hashed";
        user.status = UserStatus.ACTIVE;
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordService.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void loginRejectsSuspendedAccount() {
        UserEntity user = new UserEntity();
        user.email = "jane@example.com";
        user.passwordHash = "hashed";
        user.status = UserStatus.SUSPENDED;
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordService.matches("password123", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "password123")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void loginReturnsTokenForValidActiveUser() {
        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID();
        user.email = "jane@example.com";
        user.name = "Jane";
        user.passwordHash = "hashed";
        user.role = UserRole.SENDER;
        user.status = UserStatus.ACTIVE;
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordService.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.issue(user)).thenReturn("jwt-token");
        when(jwtService.expirySeconds()).thenReturn(86400L);

        LoginResponse response = authService.login(new LoginRequest("jane@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.expiresInSeconds()).isEqualTo(86400L);
        assertThat(response.user().email()).isEqualTo("jane@example.com");
    }

    @Test
    void verifyEmailRejectsUnknownToken() {
        when(verificationTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("bad-token")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void verifyEmailRejectsExpiredToken() {
        EmailVerificationTokenEntity token = new EmailVerificationTokenEntity();
        token.userId = UUID.randomUUID();
        token.expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);
        when(verificationTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("expired-token")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void verifyEmailMarksUserVerified() {
        UUID userId = UUID.randomUUID();
        EmailVerificationTokenEntity token = new EmailVerificationTokenEntity();
        token.userId = userId;
        token.expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        UserEntity user = new UserEntity();
        user.id = userId;
        when(verificationTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(user);

        authService.verifyEmail(new VerifyEmailRequest("good-token"));

        assertThat(user.emailVerifiedAt).isNotNull();
        assertThat(token.usedAt).isNotNull();
    }

    @Test
    void forgotPasswordDoesNothingForUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(new ForgotPasswordRequest("nobody@example.com"));

        verifyNoInteractions(mailSender);
    }

    @Test
    void forgotPasswordSendsResetEmailForKnownUser() {
        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID();
        user.email = "jane@example.com";
        user.name = "Jane";
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(new ForgotPasswordRequest("jane@example.com"));

        verify(mailSender).sendPasswordResetEmail(eq("jane@example.com"), eq("Jane"), anyString());
        verify(resetTokenRepository).persist(any(PasswordResetTokenEntity.class));
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);
        when(resetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("token", "newpassword123")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void resetPasswordUpdatesHashAndMarksTokenUsed() {
        UUID userId = UUID.randomUUID();
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.userId = userId;
        token.expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        UserEntity user = new UserEntity();
        user.id = userId;
        when(resetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(user);
        when(passwordService.hash("newpassword123")).thenReturn("new-hash");

        authService.resetPassword(new ResetPasswordRequest("token", "newpassword123"));

        assertThat(user.passwordHash).isEqualTo("new-hash");
        assertThat(token.usedAt).isNotNull();
    }
}
