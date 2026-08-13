package com.logisights.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenGeneratorTest {

    private final TokenGenerator generator = new TokenGenerator();

    @Test
    void generatesNonEmptyUrlSafeTokens() {
        String token = generator.generateRawToken();

        assertThat(token).isNotBlank();
        assertThat(token).doesNotContain("+", "/", "=");
    }

    @Test
    void generatesDifferentTokensEachCall() {
        assertThat(generator.generateRawToken()).isNotEqualTo(generator.generateRawToken());
    }

    @Test
    void hashIsDeterministicForSameInput() {
        String token = "fixed-token-value";

        assertThat(generator.hash(token)).isEqualTo(generator.hash(token));
    }

    @Test
    void hashDiffersForDifferentInput() {
        assertThat(generator.hash("token-a")).isNotEqualTo(generator.hash("token-b"));
    }

    @Test
    void hashIsNotTheRawToken() {
        String token = "some-raw-token";

        assertThat(generator.hash(token)).isNotEqualTo(token);
    }
}
