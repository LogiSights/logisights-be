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
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/payments/mpesa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Inject
    MpesaService mpesaService;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/stk-push")
    @RolesAllowed("SENDER")
    public PaymentDto initiate(@Valid StkPushInitiateRequest request) {
        return mpesaService.initiateStkPush(UUID.fromString(jwt.getSubject()), request);
    }

    /**
     * Public webhook Safaricom calls back on. Not JWT-protected — Daraja can't send a
     * bearer token — so this endpoint must stay narrow: it cross-checks the callback
     * amount against the payment we already created (see MpesaService.callbackAmountMatches),
     * and must additionally sit behind an IP allowlist / mTLS at the ingress in production.
     */
    @POST
    @Path("/callback")
    @PermitAll
    public Response callback(MpesaCallbackPayload payload) {
        mpesaService.handleCallback(payload);
        return Response.ok().build();
    }
}
