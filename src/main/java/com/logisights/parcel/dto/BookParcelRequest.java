package com.logisights.parcel.dto;

import com.logisights.common.ParcelCity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record BookParcelRequest(
        @NotBlank String recipientName,
        @NotBlank String recipientPhone,
        @Email String recipientEmail,
        @NotBlank String destinationAddress,
        @NotNull ParcelCity city,
        @NotNull @DecimalMin(value = "0.1", message = "Weight must be greater than 0") BigDecimal weightKg,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        @NotBlank String parcelType,
        UUID pickupPointId
) {
}
