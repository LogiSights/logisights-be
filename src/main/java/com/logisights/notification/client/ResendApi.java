package com.logisights.notification.client;

import com.logisights.notification.dto.ResendEmailRequest;
import com.logisights.notification.dto.ResendEmailResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Thin typed client over Resend's HTTP API (no official Java SDK).
 */
@RegisterRestClient(configKey = "resend-api")
@Path("/emails")
public interface ResendApi {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    ResendEmailResponse send(@HeaderParam("Authorization") String bearerToken, ResendEmailRequest request);
}
