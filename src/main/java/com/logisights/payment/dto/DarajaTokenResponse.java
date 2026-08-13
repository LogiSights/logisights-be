package com.logisights.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DarajaTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") String expiresIn
) {
}
