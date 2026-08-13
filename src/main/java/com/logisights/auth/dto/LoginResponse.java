package com.logisights.auth.dto;

public record LoginResponse(
        String token,
        long expiresInSeconds,
        UserDto user
) {
}
