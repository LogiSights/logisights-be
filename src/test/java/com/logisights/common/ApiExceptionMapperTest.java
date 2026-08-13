package com.logisights.common;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionMapperTest {

    private final ApiExceptionMapper mapper = new ApiExceptionMapper();

    @Test
    void mapsExceptionToJsonResponseWithMatchingStatus() {
        ApiException ex = ApiException.notFound("user not found");

        Response response = mapper.toResponse(ex);

        assertThat(response.getStatus()).isEqualTo(404);

        @SuppressWarnings("unchecked")
        var body = (java.util.Map<String, Object>) response.getEntity();
        assertThat(body.get("message")).isEqualTo("user not found");
        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("error")).isEqualTo("Not Found");
    }
}
