package com.logisights.common;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException exception) {
        return Response.status(exception.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", exception.getStatus().getStatusCode(),
                        "error", exception.getStatus().getReasonPhrase(),
                        "message", exception.getMessage()
                ))
                .build();
    }
}
