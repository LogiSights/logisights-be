package com.logisights.payment.client;

import com.logisights.payment.dto.DarajaStkPushRequest;
import com.logisights.payment.dto.DarajaStkPushResponse;
import com.logisights.payment.dto.DarajaTokenResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Thin client over Safaricom's Daraja API. All Daraja-specific request/response
 * shapes stay in this package — the rest of the codebase only sees
 * {@link com.logisights.payment.service.MpesaService}.
 */
@RegisterRestClient(configKey = "daraja-api")
public interface DarajaApi {

    @GET
    @Path("/oauth/v1/generate")
    DarajaTokenResponse getAccessToken(@QueryParam("grant_type") String grantType,
                                        @HeaderParam("Authorization") String basicAuthHeader);

    @POST
    @Path("/mpesa/stkpush/v1/processrequest")
    @Consumes(MediaType.APPLICATION_JSON)
    DarajaStkPushResponse stkPush(@HeaderParam("Authorization") String bearerToken, DarajaStkPushRequest request);
}
