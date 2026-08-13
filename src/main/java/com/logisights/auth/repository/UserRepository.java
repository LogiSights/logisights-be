package com.logisights.auth.repository;

import com.logisights.auth.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<UserEntity, UUID> {

    public Optional<UserEntity> findByEmail(String email) {
        return find("email", email.toLowerCase()).firstResultOptional();
    }

    public boolean existsByEmail(String email) {
        return count("email", email.toLowerCase()) > 0;
    }
}
