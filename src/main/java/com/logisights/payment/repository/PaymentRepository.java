package com.logisights.payment.repository;

import com.logisights.payment.entity.PaymentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PaymentRepository implements PanacheRepositoryBase<PaymentEntity, UUID> {

    public Optional<PaymentEntity> findByProviderReference(String reference) {
        return find("providerReference", reference).firstResultOptional();
    }
}
