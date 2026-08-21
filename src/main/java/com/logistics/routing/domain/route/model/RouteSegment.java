package com.logistics.routing.domain.route.model;

import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.logistics.routing.domain.route.model.enums.TransportType;

@Getter
public class RouteSegment {
    private final UUID id;
    private final Integer segmentIndex;
    private final TransportType transportType;
    private final List<Waypoint> waypoints;

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
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.segmentIndex = Objects.requireNonNull(segmentIndex, "segmentIndex must not be null");
        this.transportType = Objects.requireNonNull(transportType, "transportType must not be null");
        this.waypoints = List.copyOf(Objects.requireNonNull(waypoints, "waypoints must not be null"));
        this.plannedStartTime = Objects.requireNonNull(plannedStartTime, "plannedStartTime must not be null");
        this.plannedEndTime = Objects.requireNonNull(plannedEndTime, "plannedEndTime must not be null");
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

