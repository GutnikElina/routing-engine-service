package com.logistics.routing.application.geofence.generate;

import com.logistics.routing.domain.geofence.model.GeofenceZoneType;

import java.time.Instant;
import java.util.UUID;

/**
 * TODO(R-2): once PR #9 (OSRM routing client) merges and its {@code application.routing.GeoCoordinate}
 * type is available on main, add a {@code List<GeoCoordinate> polygon} field here (the zone's exterior
 * ring, closed) so consumers of this event don't have to look up the zone by id to get its geometry —
 * see zhenyazzz's review comment on PR #11.
 */
public record GeofenceZoneCreatedEvent(
        UUID zoneId,
        String name,
        GeofenceZoneType type,
        Instant occurredAt
) {
}
