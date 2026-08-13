package com.logisights.notification;

import java.math.BigDecimal;

/**
 * Provider-agnostic mail boundary. Other modules call this interface only —
 * nothing outside the notification package should know Resend is the concrete provider.
 */
public interface MailSender {

    // Auth
    void sendVerificationEmail(String toEmail, String recipientName, String verifyUrl);

    void sendPasswordResetEmail(String toEmail, String recipientName, String resetUrl);

    // Parcel lifecycle (sender-facing)
    void sendBookingConfirmation(String toEmail, String recipientName, String trackingId,
                                  BigDecimal costKes, String destinationAddress, String trackUrl);

    void sendStatusUpdate(String toEmail, String recipientName, String trackingId,
                           String status, String note, String trackUrl);

    void sendDeliveryReceipt(String toEmail, String recipientName, String trackingId,
                              BigDecimal costKes, String trackUrl);

    // Payment
    void sendPaymentConfirmation(String toEmail, String recipientName, String trackingId, BigDecimal amountKes);

    void sendPaymentFailed(String toEmail, String recipientName, String trackingId);

    // Pickup
    void sendPickupReady(String toEmail, String recipientName, String trackingId,
                          String pickupPointName, String pickupPointAddress);

    // Driver
    void sendDriverAssigned(String toEmail, String driverName, String trackingId, String destinationAddress);

    void sendWeeklyEarningsDigest(String toEmail, String driverName, BigDecimal totalKes,
                                   long deliveries, String weekLabel);

    // Admin / account
    void sendAdminNewUserAlert(String adminEmail, String newUserName, String newUserEmail, String role);

    void sendAccountSuspended(String toEmail, String recipientName);
}
