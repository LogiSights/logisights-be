package com.logisights.pickup.entity;

import com.logisights.common.PickupActivityAction;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pickup_activity_log")
public class PickupActivityLogEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "pickup_point_id")
    public UUID pickupPointId;

    @Column(name = "staff_user_id")
    public UUID staffUserId;

    @Column(name = "parcel_id")
    public UUID parcelId;

    @Enumerated(EnumType.STRING)
    public PickupActivityAction action;

    public String type;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}
