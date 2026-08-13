package com.logisights.pickup.dto;

import com.logisights.common.PickupActivityAction;
import com.logisights.pickup.entity.PickupActivityLogEntity;

import java.time.Instant;
import java.util.UUID;

public record PickupActivityDto(
        UUID id,
        UUID pickupPointId,
        UUID staffUserId,
        UUID parcelId,
        PickupActivityAction action,
        String type,
        Instant createdAt
) {
    public static PickupActivityDto from(PickupActivityLogEntity e) {
        return new PickupActivityDto(e.id, e.pickupPointId, e.staffUserId, e.parcelId, e.action, e.type, e.createdAt);
    }
}
