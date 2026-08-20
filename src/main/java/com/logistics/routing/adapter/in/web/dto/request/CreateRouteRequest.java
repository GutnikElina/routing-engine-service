package com.logistics.routing.adapter.in.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRouteRequest(
    @NotBlank
    @Size(max = 64)
    String orderNumber,

    @NotNull
    @Valid
    CargoRequest cargo,

    @NotEmpty
    @Size(min = 2, max = 100)
    List<@Valid WaypointRequest> waypoints
) {
}
