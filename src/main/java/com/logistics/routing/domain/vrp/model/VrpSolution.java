package com.logistics.routing.domain.vrp.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

public record VrpSolution(
        List<VrpVehicleRoute> routes,
        List<Integer> unassignedStopIndices,
        BigDecimal totalDistanceMeters,
        Duration totalDuration
) {

    public VrpSolution {
        routes = List.copyOf(routes);
        unassignedStopIndices = List.copyOf(unassignedStopIndices);
    }
}
