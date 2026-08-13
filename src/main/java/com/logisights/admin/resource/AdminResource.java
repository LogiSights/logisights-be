package com.logisights.admin.resource;

import com.logisights.admin.dto.*;
import com.logisights.admin.service.AdminService;
import com.logisights.auth.dto.UserDto;
import com.logisights.parcel.dto.ParcelDto;
import com.logisights.parcel.service.ParcelService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminResource {

    @Inject
    AdminService adminService;

    @Inject
    ParcelService parcelService;

    @PATCH
    @Path("/parcels/{id}/assign-driver")
    public ParcelDto assignDriver(@PathParam("id") UUID id, @Valid AssignDriverRequest request) {
        return parcelService.assignDriver(id, request.driverId());
    }

    @GET
    @Path("/users")
    public List<UserDto> listUsers() {
        return adminService.listUsers();
    }

    @PATCH
    @Path("/users/{id}/status")
    public UserDto updateUserStatus(@PathParam("id") UUID id, @Valid UpdateUserStatusRequest request) {
        return adminService.updateUserStatus(id, request.status());
    }

    @GET
    @Path("/stats/summary")
    public AdminStatsSummaryDto summary() {
        return adminService.summary();
    }

    @GET
    @Path("/stats/delivery-trend")
    public List<DeliveryTrendPointDto> deliveryTrend(@QueryParam("days") @DefaultValue("14") int days) {
        return adminService.deliveryTrend(days);
    }

    @GET
    @Path("/stats/status-breakdown")
    public List<StatusBreakdownDto> statusBreakdown() {
        return adminService.statusBreakdown();
    }

    @GET
    @Path("/stats/top-cities")
    public List<TopCityDto> topCities() {
        return adminService.topCities();
    }
}
