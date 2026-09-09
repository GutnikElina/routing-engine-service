package com.logistics.routing.application.geofence.generate;

import com.logistics.routing.domain.geofence.model.GeofenceZoneType;

import java.math.BigDecimal;
import java.util.UUID;

public record GenerateGeofenceZoneCommand(
        String name,
        GeofenceZoneType type,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal radiusMeters,
        UUID sourceWaypointId
) {
}
