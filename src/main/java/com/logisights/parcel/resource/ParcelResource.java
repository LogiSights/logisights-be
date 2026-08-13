package com.logisights.parcel.resource;

import com.logisights.parcel.dto.BookParcelRequest;
import com.logisights.parcel.dto.ParcelDto;
import com.logisights.parcel.dto.UpdateStatusRequest;
import com.logisights.parcel.service.ParcelService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/parcels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ParcelResource {

    @Inject
    ParcelService parcelService;

    @Inject
    JsonWebToken jwt;

    @POST
    @RolesAllowed("SENDER")
    public Response book(@Valid BookParcelRequest request) {
        ParcelDto parcel = parcelService.book(currentUserId(), request);
        return Response.status(Response.Status.CREATED).entity(parcel).build();
    }

    @GET
    @Path("/{trackingId}")
    @RolesAllowed({"SENDER", "DRIVER", "PICKUP", "ADMIN"})
    public ParcelDto getByTrackingId(@PathParam("trackingId") String trackingId) {
        return parcelService.getByTrackingId(trackingId);
    }

    @GET
    @RolesAllowed("SENDER")
    public List<ParcelDto> listMine() {
        return parcelService.listForSender(currentUserId());
    }

    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({"DRIVER", "PICKUP", "ADMIN"})
    public ParcelDto updateStatus(@PathParam("id") UUID id, @Valid UpdateStatusRequest request) {
        return parcelService.updateStatus(id, currentUserId(), request);
    }

    private UUID currentUserId() {
        return UUID.fromString(jwt.getSubject());
    }
}
