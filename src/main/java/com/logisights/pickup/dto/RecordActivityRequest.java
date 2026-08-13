package com.logisights.pickup.dto;

import com.logisights.common.PickupActivityAction;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecordActivityRequest(
        @NotNull UUID parcelId,
        @NotNull UUID pickupPointId,
        @NotNull PickupActivityAction action,
        String type
) {
}
