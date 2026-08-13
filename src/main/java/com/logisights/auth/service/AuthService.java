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
import com.logisights.notification.MailSender;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    EmailVerificationTokenRepository verificationTokenRepository;

    @Inject
    PasswordResetTokenRepository resetTokenRepository;

    @Inject
    PasswordService passwordService;

    @Inject
    JwtService jwtService;

    @Inject
    TokenGenerator tokenGenerator;

    @Inject
    MailSender mailSender;

    @ConfigProperty(name = "app.frontend-base-url")
    String frontendBaseUrl;

    @ConfigProperty(name = "app.admin-alert-email")
    String adminAlertEmail;

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw ApiException.forbidden("Admin accounts cannot be self-registered");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("An account with this email already exists");
        }

        UserEntity user = new UserEntity();
        user.name = request.name();
        user.email = request.email().toLowerCase();
        user.phone = request.phone();
        user.role = request.role();
        user.passwordHash = passwordService.hash(request.password());
        userRepository.persist(user);

        issueVerificationEmail(user);
        mailSender.sendAdminNewUserAlert(adminAlertEmail, user.name, user.email, user.role.name());

        return UserDto.from(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (!passwordService.matches(request.password(), user.passwordHash)) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        if (user.status == com.logisights.common.UserStatus.SUSPENDED) {
            throw ApiException.forbidden("This account has been suspended");
        }

        String token = jwtService.issue(user);
        return new LoginResponse(token, jwtService.expirySeconds(), UserDto.from(user));
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String hash = tokenGenerator.hash(request.token());
        EmailVerificationTokenEntity tokenEntity = verificationTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired verification token"));

        if (tokenEntity.usedAt != null || tokenEntity.expiresAt.isBefore(Instant.now())) {
            throw ApiException.badRequest("Invalid or expired verification token");
        }

        UserEntity user = userRepository.findById(tokenEntity.userId);
        if (user == null) {
            throw ApiException.notFound("User not found");
        }

        user.emailVerifiedAt = Instant.now();
        tokenEntity.usedAt = Instant.now();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always respond as if it worked, whether or not the email exists — avoids account enumeration.
        userRepository.findByEmail(request.email()).ifPresent(this::issuePasswordResetEmail);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hash = tokenGenerator.hash(request.token());
        PasswordResetTokenEntity tokenEntity = resetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset token"));

        if (tokenEntity.usedAt != null || tokenEntity.expiresAt.isBefore(Instant.now())) {
            throw ApiException.badRequest("Invalid or expired reset token");
        }

        UserEntity user = userRepository.findById(tokenEntity.userId);
        if (user == null) {
            throw ApiException.notFound("User not found");
        }

        user.passwordHash = passwordService.hash(request.newPassword());
        tokenEntity.usedAt = Instant.now();
    }

    private void issueVerificationEmail(UserEntity user) {
        String rawToken = tokenGenerator.generateRawToken();
        EmailVerificationTokenEntity tokenEntity = new EmailVerificationTokenEntity();
        tokenEntity.userId = user.id;
        tokenEntity.tokenHash = tokenGenerator.hash(rawToken);
        tokenEntity.expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        verificationTokenRepository.persist(tokenEntity);

        String verifyUrl = frontendBaseUrl + "/verify-email?token=" + rawToken;
        mailSender.sendVerificationEmail(user.email, user.name, verifyUrl);
    }

    private void issuePasswordResetEmail(UserEntity user) {
        String rawToken = tokenGenerator.generateRawToken();
        PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity();
        tokenEntity.userId = user.id;
        tokenEntity.tokenHash = tokenGenerator.hash(rawToken);
        tokenEntity.expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        resetTokenRepository.persist(tokenEntity);

        String resetUrl = frontendBaseUrl + "/reset-password?token=" + rawToken;
        mailSender.sendPasswordResetEmail(user.email, user.name, resetUrl);
    }
}
