package com.logisights.payment.dto;

import com.logisights.common.PaymentStatus;
import com.logisights.payment.entity.PaymentEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentDto(
        UUID id,
        UUID parcelId,
        String providerReference,
        BigDecimal amountKes,
        PaymentStatus status
) {
    public static PaymentDto from(PaymentEntity e) {
        return new PaymentDto(e.id, e.parcelId, e.providerReference, e.amountKes, e.status);
    }
}
