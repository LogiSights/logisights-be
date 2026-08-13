package com.logisights.pickup.entity;

import com.logisights.common.PickupItemStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pickup_inventory")
public class PickupInventoryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "parcel_id")
    public UUID parcelId;

    @Column(name = "pickup_point_id")
    public UUID pickupPointId;

    @Column(name = "date_arrived")
    public Instant dateArrived = Instant.now();

    @Enumerated(EnumType.STRING)
    public PickupItemStatus status = PickupItemStatus.AWAITING_PICKUP;
}
