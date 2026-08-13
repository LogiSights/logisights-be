package com.logisights.parcel.entity;

import com.logisights.common.ParcelCity;
import com.logisights.common.ParcelStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parcels")
public class ParcelEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "tracking_id")
    public String trackingId;

    @Column(name = "sender_id")
    public UUID senderId;

    @Column(name = "recipient_name")
    public String recipientName;

    @Column(name = "recipient_phone")
    public String recipientPhone;

    @Column(name = "destination_address")
    public String destinationAddress;

    @Enumerated(EnumType.STRING)
    public ParcelCity city;

    @Column(name = "weight_kg")
    public BigDecimal weightKg;

    @Column(name = "length_cm")
    public BigDecimal lengthCm;

    @Column(name = "width_cm")
    public BigDecimal widthCm;

    @Column(name = "height_cm")
    public BigDecimal heightCm;

    @Column(name = "parcel_type")
    public String parcelType;

    @Column(name = "pickup_point_id")
    public UUID pickupPointId;

    @Enumerated(EnumType.STRING)
    public ParcelStatus status = ParcelStatus.PENDING;

    @Column(name = "cost_kes")
    public BigDecimal costKes;

    @Column(name = "driver_id")
    public UUID driverId;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    public Instant updatedAt = Instant.now();

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
