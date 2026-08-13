package com.logisights.payment.dto;

public record DarajaStkPushResponse(
        String MerchantRequestID,
        String CheckoutRequestID,
        String ResponseCode,
        String ResponseDescription,
        String CustomerMessage
) {
}
