package com.logisights.pickup.resource;

import com.logisights.pickup.dto.PickupActivityDto;
import com.logisights.pickup.dto.PickupInventoryDto;
import com.logisights.pickup.dto.RecordActivityRequest;
import com.logisights.pickup.service.PickupService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/pickup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("PICKUP")
public class PickupResource {

    @Inject
    PickupService pickupService;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/inventory")
    public List<PickupInventoryDto> inventory(@QueryParam("pickupPointId") UUID pickupPointId) {
        return pickupService.inventoryFor(pickupPointId);
    }

    @POST
    @Path("/activity")
    public PickupActivityDto recordActivity(@Valid RecordActivityRequest request) {
        UUID staffUserId = UUID.fromString(jwt.getSubject());
        return pickupService.recordActivity(staffUserId, request);
    }
}
