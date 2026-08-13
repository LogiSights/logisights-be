package com.logisights.notification;

import com.logisights.notification.client.ResendApi;
import com.logisights.notification.dto.ResendEmailRequest;
import io.quarkus.logging.Log;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@ApplicationScoped
public class ResendMailSender implements MailSender {

    @CheckedTemplate(basePath = "notification")
    static class Templates {
        static native TemplateInstance verifyEmail(String name, String verifyUrl);

        static native TemplateInstance passwordReset(String name, String resetUrl);
    }

    @RestClient
    ResendApi resendApi;

    @ConfigProperty(name = "resend.api-key")
    String apiKey;

    @ConfigProperty(name = "mail.from-address")
    String fromAddress;

    @Override
    public void sendVerificationEmail(String toEmail, String recipientName, String verifyUrl) {
        String html = Templates.verifyEmail(recipientName, verifyUrl).render();
        send(toEmail, "Confirm your Logisights email address", html);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetUrl) {
        String html = Templates.passwordReset(recipientName, resetUrl).render();
        send(toEmail, "Reset your Logisights password", html);
    }

    private void send(String toEmail, String subject, String html) {
        var request = new ResendEmailRequest(fromAddress, List.of(toEmail), subject, html, null);
        try {
            resendApi.send("Bearer " + apiKey, request);
        } catch (Exception e) {
            // Never let a mail-provider failure break the auth/payment flow that triggered it.
            Log.error("Failed to send email via Resend to " + toEmail, e);
        }
    }
}
