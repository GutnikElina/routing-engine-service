package com.logistics.routing.application.route.create;

import java.math.BigDecimal;
import java.time.Instant;

import com.logistics.routing.domain.route.WaypointType;

public record WaypointCommand(
    WaypointType type,
    Integer sequence,
    BigDecimal latitude,
    BigDecimal longitude,
    String address,
    Instant timeWindowStart,
    Instant timeWindowEnd
) {

}
