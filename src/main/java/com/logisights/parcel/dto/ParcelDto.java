package com.logisights.parcel.dto;

import com.logisights.common.ParcelCity;
import com.logisights.common.ParcelStatus;
import com.logisights.parcel.entity.ParcelEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ParcelDto(
        UUID id,
        String trackingId,
        UUID senderId,
        String recipientName,
        String recipientPhone,
        String destinationAddress,
        ParcelCity city,
        BigDecimal weightKg,
        String parcelType,
        UUID pickupPointId,
        ParcelStatus status,
        BigDecimal costKes,
        UUID driverId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ParcelDto from(ParcelEntity e) {
        return new ParcelDto(e.id, e.trackingId, e.senderId, e.recipientName, e.recipientPhone,
                e.destinationAddress, e.city, e.weightKg, e.parcelType, e.pickupPointId,
                e.status, e.costKes, e.driverId, e.createdAt, e.updatedAt);
    }
}
