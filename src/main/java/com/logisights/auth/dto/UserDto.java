package com.logisights.auth.dto;

import com.logisights.auth.entity.UserEntity;
import com.logisights.common.UserRole;
import com.logisights.common.UserStatus;

import java.util.UUID;

public record UserDto(
        UUID id,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        String avatarUrl,
        boolean emailVerified
) {
    public static UserDto from(UserEntity entity) {
        return new UserDto(
                entity.id,
                entity.name,
                entity.email,
                entity.phone,
                entity.role,
                entity.status,
                entity.avatarUrl,
                entity.emailVerifiedAt != null
        );
    }
}
