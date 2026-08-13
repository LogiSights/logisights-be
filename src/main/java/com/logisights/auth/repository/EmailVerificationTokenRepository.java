package com.logisights.auth.repository;

import com.logisights.auth.entity.EmailVerificationTokenEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EmailVerificationTokenRepository implements PanacheRepositoryBase<EmailVerificationTokenEntity, UUID> {

    public Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }
}
