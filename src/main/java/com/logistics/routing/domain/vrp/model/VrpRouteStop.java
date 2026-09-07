package com.logistics.routing.domain.vrp.model;

import java.time.Instant;

public record VrpRouteStop(
        int locationIndex,
        Instant arrivalTime,
        Instant departureTime
) {
}
