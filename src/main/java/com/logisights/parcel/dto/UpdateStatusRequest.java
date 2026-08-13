package com.logisights.parcel.dto;

import com.logisights.common.ParcelStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull ParcelStatus status,
        String note
) {
}
