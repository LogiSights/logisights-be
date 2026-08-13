package com.logisights.driver.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_earnings")
public class DriverEarningsEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "driver_id")
    public UUID driverId;

    @Column(name = "parcel_id")
    public UUID parcelId;

    @Column(name = "amount_kes")
    public BigDecimal amountKes;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}
