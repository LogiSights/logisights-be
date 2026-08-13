package com.logisights.auth.resource;

import com.logisights.auth.dto.*;
import com.logisights.auth.service.AuthService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequest request) {
        UserDto user = authService.register(request);
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @POST
    @Path("/login")
    public LoginResponse login(@Valid LoginRequest request) {
        return authService.login(request);
    }

    @POST
    @Path("/verify-email")
    public Response verifyEmail(@Valid VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return Response.ok(Map.of("message", "Email verified successfully")).build();
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(@Valid ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return Response.ok(Map.of("message", "If that email exists, a reset link has been sent")).build();
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(@Valid ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Response.ok(Map.of("message", "Password reset successfully")).build();
    }
}
