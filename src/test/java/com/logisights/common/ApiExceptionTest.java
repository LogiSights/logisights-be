package com.logisights.common;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionTest {

    @Test
    void notFoundHasNotFoundStatus() {
        ApiException ex = ApiException.notFound("missing");

        assertThat(ex.getStatus()).isEqualTo(Response.Status.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("missing");
    }

    @Test
    void badRequestHasBadRequestStatus() {
        assertThat(ApiException.badRequest("bad").getStatus()).isEqualTo(Response.Status.BAD_REQUEST);
    }

    @Test
    void conflictHasConflictStatus() {
        assertThat(ApiException.conflict("dup").getStatus()).isEqualTo(Response.Status.CONFLICT);
    }

    @Test
    void unauthorizedHasUnauthorizedStatus() {
        assertThat(ApiException.unauthorized("nope").getStatus()).isEqualTo(Response.Status.UNAUTHORIZED);
    }

    @Test
    void forbiddenHasForbiddenStatus() {
        assertThat(ApiException.forbidden("no").getStatus()).isEqualTo(Response.Status.FORBIDDEN);
    }
}
