package com.logistics.routing.domain.geofence.model;

import lombok.Getter;
import org.locationtech.jts.geom.Polygon;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class GeofenceZone {
    private UUID id;
    private final String name;
    private final GeofenceZoneType type;
    private final UUID sourceWaypointId;
    private final Polygon polygon;
    private final Instant createdAt;

    private GeofenceZone(
            UUID id,
            String name,
            GeofenceZoneType type,
            UUID sourceWaypointId,
            Polygon polygon,
            Instant createdAt
    ) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.sourceWaypointId = sourceWaypointId;
        this.polygon = Objects.requireNonNull(polygon, "polygon must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static GeofenceZone create(
            UUID id,
            String name,
            GeofenceZoneType type,
            UUID sourceWaypointId,
            Polygon polygon,
            Instant createdAt
    ) {
        return new GeofenceZone(id, name, type, sourceWaypointId, polygon, createdAt);
    }
}
