package com.logisights.driver.resource;

import com.logisights.driver.dto.DriverEarningsSummaryDto;
import com.logisights.driver.service.DriverService;
import com.logisights.parcel.dto.ParcelDto;
import com.logisights.parcel.dto.UpdateStatusRequest;
import com.logisights.parcel.service.ParcelService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/driver")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("DRIVER")
public class DriverResource {

    @Inject
    ParcelService parcelService;

    @Inject
    DriverService driverService;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/tasks")
    public List<ParcelDto> myTasks() {
        return parcelService.listForDriver(currentUserId());
    }

    @PATCH
    @Path("/tasks/{id}/status")
    public ParcelDto updateTaskStatus(@PathParam("id") UUID id, @Valid UpdateStatusRequest request) {
        return parcelService.updateStatus(id, currentUserId(), request);
    }

    @GET
    @Path("/earnings")
    public DriverEarningsSummaryDto earnings() {
        return driverService.earningsSummary(currentUserId());
    }

    private UUID currentUserId() {
        return UUID.fromString(jwt.getSubject());
    }
}
