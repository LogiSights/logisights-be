package com.logisights.notification;

import com.logisights.notification.client.ResendApi;
import com.logisights.notification.dto.ResendEmailRequest;
import io.quarkus.logging.Log;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class ResendMailSender implements MailSender {

    @CheckedTemplate(basePath = "notification")
    static class Templates {
        static native TemplateInstance verifyEmail(String name, String verifyUrl);

        static native TemplateInstance passwordReset(String name, String resetUrl);

        static native TemplateInstance bookingConfirmation(String name, String trackingId, BigDecimal costKes,
                                                             String destinationAddress, String trackUrl);

        static native TemplateInstance statusUpdate(String name, String trackingId, String status, String note,
                                                      String trackUrl);

        static native TemplateInstance deliveryReceipt(String name, String trackingId, BigDecimal costKes,
                                                         String trackUrl);

        static native TemplateInstance paymentConfirmation(String name, String trackingId, BigDecimal amountKes);

        static native TemplateInstance paymentFailed(String name, String trackingId);

        static native TemplateInstance pickupReady(String name, String trackingId, String pickupPointName,
                                                     String pickupPointAddress);

        static native TemplateInstance driverAssigned(String name, String trackingId, String destinationAddress);

        static native TemplateInstance weeklyEarningsDigest(String name, BigDecimal totalKes, long deliveries,
                                                              String weekLabel);

        static native TemplateInstance adminNewUserAlert(String newUserName, String newUserEmail, String role);

        static native TemplateInstance accountSuspended(String name);
    }

    @RestClient
    ResendApi resendApi;

    @ConfigProperty(name = "resend.api-key")
    String apiKey;

    @ConfigProperty(name = "mail.from-address")
    String fromAddress;

    @Override
    public void sendVerificationEmail(String toEmail, String recipientName, String verifyUrl) {
        send(toEmail, "Confirm your Logisights email address",
                Templates.verifyEmail(recipientName, verifyUrl).render());
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetUrl) {
        send(toEmail, "Reset your Logisights password",
                Templates.passwordReset(recipientName, resetUrl).render());
    }

    @Override
    public void sendBookingConfirmation(String toEmail, String recipientName, String trackingId,
                                         BigDecimal costKes, String destinationAddress, String trackUrl) {
        send(toEmail, "Parcel " + trackingId + " booked",
                Templates.bookingConfirmation(recipientName, trackingId, costKes, destinationAddress, trackUrl).render());
    }

    @Override
    public void sendStatusUpdate(String toEmail, String recipientName, String trackingId, String status,
                                  String note, String trackUrl) {
        send(toEmail, "Parcel " + trackingId + " is now " + status,
                Templates.statusUpdate(recipientName, trackingId, status, note, trackUrl).render());
    }

    @Override
    public void sendDeliveryReceipt(String toEmail, String recipientName, String trackingId,
                                     BigDecimal costKes, String trackUrl) {
        send(toEmail, "Delivered — receipt for " + trackingId,
                Templates.deliveryReceipt(recipientName, trackingId, costKes, trackUrl).render());
    }

    @Override
    public void sendPaymentConfirmation(String toEmail, String recipientName, String trackingId, BigDecimal amountKes) {
        send(toEmail, "Payment received for " + trackingId,
                Templates.paymentConfirmation(recipientName, trackingId, amountKes).render());
    }

    @Override
    public void sendPaymentFailed(String toEmail, String recipientName, String trackingId) {
        send(toEmail, "Payment failed for " + trackingId,
                Templates.paymentFailed(recipientName, trackingId).render());
    }

    @Override
    public void sendPickupReady(String toEmail, String recipientName, String trackingId,
                                 String pickupPointName, String pickupPointAddress) {
        send(toEmail, "Parcel " + trackingId + " is ready for pickup",
                Templates.pickupReady(recipientName, trackingId, pickupPointName, pickupPointAddress).render());
    }

    @Override
    public void sendDriverAssigned(String toEmail, String driverName, String trackingId, String destinationAddress) {
        send(toEmail, "New delivery assigned: " + trackingId,
                Templates.driverAssigned(driverName, trackingId, destinationAddress).render());
    }

    @Override
    public void sendWeeklyEarningsDigest(String toEmail, String driverName, BigDecimal totalKes,
                                          long deliveries, String weekLabel) {
        send(toEmail, "Your Logisights earnings, " + weekLabel,
                Templates.weeklyEarningsDigest(driverName, totalKes, deliveries, weekLabel).render());
    }

    @Override
    public void sendAdminNewUserAlert(String adminEmail, String newUserName, String newUserEmail, String role) {
        send(adminEmail, "New user registered: " + newUserName,
                Templates.adminNewUserAlert(newUserName, newUserEmail, role).render());
    }

    @Override
    public void sendAccountSuspended(String toEmail, String recipientName) {
        send(toEmail, "Your Logisights account has been suspended",
                Templates.accountSuspended(recipientName).render());
    }

    private void send(String toEmail, String subject, String html) {
        var request = new ResendEmailRequest(fromAddress, List.of(toEmail), subject, html, null);
        try {
            resendApi.send("Bearer " + apiKey, request);
        } catch (Exception e) {
            // Never let a mail-provider failure break the flow that triggered it.
            Log.error("Failed to send email via Resend to " + toEmail, e);
        }
    }
}
