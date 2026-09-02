package com.logistics.routing.application.routing;

import java.math.BigDecimal;
import java.util.List;

public record RouteGeometry(
        List<GeoCoordinate> path,
        BigDecimal distanceMeters,
        BigDecimal durationSeconds
) {
}
