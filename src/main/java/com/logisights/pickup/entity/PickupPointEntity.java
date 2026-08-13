package com.logisights.pickup.entity;

import com.logisights.common.ParcelCity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pickup_points")
public class PickupPointEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    public String name;

    public String address;

    @Enumerated(EnumType.STRING)
    public ParcelCity city;

    @Column(name = "staff_user_id")
    public UUID staffUserId;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}
