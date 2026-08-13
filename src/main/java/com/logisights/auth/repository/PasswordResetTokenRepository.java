package com.logisights.auth.repository;

import com.logisights.auth.entity.PasswordResetTokenEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PasswordResetTokenRepository implements PanacheRepositoryBase<PasswordResetTokenEntity, UUID> {

    public Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }
}
