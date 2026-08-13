package com.logisights.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordServiceTest {

    private final PasswordService service = new PasswordService();

    @Test
    void hashedPasswordMatchesOriginal() {
        String hash = service.hash("correct-horse-battery-staple");

        assertThat(service.matches("correct-horse-battery-staple", hash)).isTrue();
    }

    @Test
    void wrongPasswordDoesNotMatch() {
        String hash = service.hash("correct-horse-battery-staple");

        assertThat(service.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void hashIsNotThePlaintextPassword() {
        String hash = service.hash("my-password");

        assertThat(hash).isNotEqualTo("my-password");
    }
}
