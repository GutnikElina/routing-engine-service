package com.logistics.routing.domain.route.model;

import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.logistics.routing.domain.route.exception.RouteSegmentationException;
import com.logistics.routing.domain.route.model.enums.TransportType;

@Getter
public class RouteSegment {
    private UUID id;
    private final Integer segmentIndex;
    private final TransportType transportType;
    private final List<Waypoint> waypoints;

    // Null until a schedule is computed for the route (mirrors RouteOrder.totalDistanceKm/eta).
    private final Instant plannedStartTime;
    private final Instant plannedEndTime;
    private Instant actualStartTime;
    private Instant actualEndTime;

    private RouteSegment(
        UUID id,
        Integer segmentIndex,
        TransportType transportType,
        List<Waypoint> waypoints,
        Instant plannedStartTime,
        Instant plannedEndTime,
        Instant actualStartTime,
        Instant actualEndTime
    ) {
        this.id = id;
        this.segmentIndex = Objects.requireNonNull(segmentIndex, "segmentIndex must not be null");
        this.transportType = Objects.requireNonNull(transportType, "transportType must not be null");
        this.waypoints = List.copyOf(Objects.requireNonNull(waypoints, "waypoints must not be null"));
        if (this.waypoints.size() < 2) {
            throw new RouteSegmentationException("a route segment must span at least two waypoints");
        }
        this.plannedStartTime = plannedStartTime;
        this.plannedEndTime = plannedEndTime;
        this.actualStartTime = actualStartTime;
        this.actualEndTime = actualEndTime;
    }

    public static RouteSegment create(

        UUID id,
        Integer segmentIndex,
        TransportType transportType,
        List<Waypoint> waypoints,
        Instant plannedStartTime,
        Instant plannedEndTime,
        Instant actualStartTime,
        Instant actualEndTime
    ) {
        return new RouteSegment(
            id, 
            segmentIndex, 
            transportType,
            waypoints,
            plannedStartTime,
            plannedEndTime, 
            actualStartTime, 
            actualEndTime
        );
    }
}

