package com.logisights.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record StkPushInitiateRequest(
        @NotNull UUID parcelId,
        @NotBlank @Pattern(regexp = "^254[17]\\d{8}$", message = "Phone must be in format 2547XXXXXXXX or 2541XXXXXXXX")
        String phone
) {
}
