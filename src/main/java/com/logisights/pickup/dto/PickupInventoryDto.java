package com.logisights.pickup.dto;

import com.logisights.common.PickupItemStatus;
import com.logisights.pickup.entity.PickupInventoryEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record PickupInventoryDto(
        UUID id,
        UUID parcelId,
        UUID pickupPointId,
        Instant dateArrived,
        long daysWaiting,
        PickupItemStatus status
) {
    public static PickupInventoryDto from(PickupInventoryEntity e) {
        long days = Duration.between(e.dateArrived, Instant.now()).toDays();
        return new PickupInventoryDto(e.id, e.parcelId, e.pickupPointId, e.dateArrived, days, e.status);
    }
}
