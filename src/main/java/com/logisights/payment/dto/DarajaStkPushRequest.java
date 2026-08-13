package com.logisights.payment.dto;

public record DarajaStkPushRequest(
        String BusinessShortCode,
        String Password,
        String Timestamp,
        String TransactionType,
        String Amount,
        String PartyA,
        String PartyB,
        String PhoneNumber,
        String CallBackURL,
        String AccountReference,
        String TransactionDesc
) {
}
