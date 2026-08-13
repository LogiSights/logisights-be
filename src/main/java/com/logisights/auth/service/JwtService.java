package com.logisights.auth.service;

import com.logisights.auth.entity.UserEntity;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class JwtService {

    @ConfigProperty(name = "logisights.jwt.expiry-minutes")
    long expiryMinutes;

    public String issue(UserEntity user) {
        return Jwt.issuer("https://logisights.co.ke")
                .subject(user.id.toString())
                .claim("email", user.email)
                .claim("name", user.name)
                .groups(user.role.name())
                .expiresIn(Duration.ofMinutes(expiryMinutes))
                .sign();
    }

    public long expirySeconds() {
        return Duration.ofMinutes(expiryMinutes).toSeconds();
    }
}
