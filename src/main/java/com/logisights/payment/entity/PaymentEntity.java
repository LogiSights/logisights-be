package com.logisights.payment.entity;

import com.logisights.common.PaymentProvider;
import com.logisights.common.PaymentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "parcel_id")
    public UUID parcelId;

    @Enumerated(EnumType.STRING)
    public PaymentProvider provider = PaymentProvider.MPESA;

    @Column(name = "provider_reference")
    public String providerReference;

    public String phone;

    @Column(name = "amount_kes")
    public BigDecimal amountKes;

    @Enumerated(EnumType.STRING)
    public PaymentStatus status = PaymentStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_callback", columnDefinition = "jsonb")
    public String rawCallback;

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
