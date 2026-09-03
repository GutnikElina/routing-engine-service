package com.logistics.routing.domain.vrp.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

public record VrpVehicleRoute(
        String vehicleId,
        List<VrpRouteStop> stops,
        BigDecimal totalDistanceMeters,
        Duration totalDuration,
        BigDecimal totalWeightKg,
        BigDecimal totalVolumeM3
) {

    public VrpVehicleRoute {
        stops = List.copyOf(stops);
    }
}
