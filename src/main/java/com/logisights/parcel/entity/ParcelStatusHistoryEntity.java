package com.logisights.parcel.entity;

import com.logisights.common.ParcelStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parcel_status_history")
public class ParcelStatusHistoryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "parcel_id")
    public UUID parcelId;

    @Enumerated(EnumType.STRING)
    public ParcelStatus status;

    @Column(name = "changed_by")
    public UUID changedBy;

    public String note;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}
