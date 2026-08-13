package com.logisights.auth.entity;

import com.logisights.common.UserRole;
import com.logisights.common.UserStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    public String name;

    public String email;

    @Column(name = "password_hash")
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    public UserRole role;

    public String phone;

    @Column(name = "avatar_url")
    public String avatarUrl;

    @Enumerated(EnumType.STRING)
    public UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified_at")
    public Instant emailVerifiedAt;

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
