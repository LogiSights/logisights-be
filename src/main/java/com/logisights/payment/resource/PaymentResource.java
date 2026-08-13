package com.logisights.payment.resource;

import com.logisights.payment.dto.MpesaCallbackPayload;
import com.logisights.payment.dto.PaymentDto;
import com.logisights.payment.dto.StkPushInitiateRequest;
import com.logisights.payment.service.MpesaService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/payments/mpesa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Inject
    MpesaService mpesaService;

    @POST
    @Path("/stk-push")
    @RolesAllowed("SENDER")
    public PaymentDto initiate(@Valid StkPushInitiateRequest request) {
        return mpesaService.initiateStkPush(request);
    }

    /**
     * Public webhook Safaricom calls back on. Not JWT-protected — Daraja can't send a
     * bearer token — so this endpoint must stay narrow: it only accepts a callback shape
     * matching a payment we already created, and must sit behind an IP allowlist / mTLS
     * at the ingress in production.
     */
    @POST
    @Path("/callback")
    @PermitAll
    public Response callback(MpesaCallbackPayload payload) {
        mpesaService.handleCallback(payload);
        return Response.ok().build();
    }
}
