package com.logisights.notification;

/**
 * Provider-agnostic mail boundary. auth/payment call this interface only —
 * nothing outside this package should know Resend is the concrete provider.
 */
public interface MailSender {

    void sendVerificationEmail(String toEmail, String recipientName, String verifyUrl);

    void sendPasswordResetEmail(String toEmail, String recipientName, String resetUrl);
}
