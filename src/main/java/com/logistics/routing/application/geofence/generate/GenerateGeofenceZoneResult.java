package com.logistics.routing.application.geofence.generate;

import com.logistics.routing.domain.geofence.model.GeofenceZoneType;

import java.time.Instant;
import java.util.UUID;

public record GenerateGeofenceZoneResult(
        UUID zoneId,
        String name,
        GeofenceZoneType type,
        Instant createdAt
) {
}
