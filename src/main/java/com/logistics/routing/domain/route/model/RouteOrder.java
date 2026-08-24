package com.logistics.routing.domain.route.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.logistics.routing.domain.route.exception.InvalidRouteDraftException;
import com.logistics.routing.domain.route.model.enums.AdrClass;
import com.logistics.routing.domain.route.model.enums.RouteOrderStatus;
import com.logistics.routing.domain.route.model.enums.WaypointType;


@Getter
public class RouteOrder {
    private UUID id;
    private final String orderNumber;
    private RouteOrderStatus status;
    private final BigDecimal cargoWeightKg;
    private final BigDecimal cargoVolumeM3;
    private final AdrClass adrClass;
    private final BigDecimal temperatureMin;
    private final BigDecimal temperatureMax;
    private final List<Waypoint> waypoints;
    private final List<RouteSegment> segments;
    private BigDecimal totalDistanceKm;
    private BigDecimal totalCost;
    private Instant eta;
    private final Instant createdAt;
    private Instant updatedAt;

    private RouteOrder(
            UUID id,
            String orderNumber,
            RouteOrderStatus status,
            BigDecimal cargoWeightKg,
            BigDecimal cargoVolumeM3,
            AdrClass adrClass,
            BigDecimal temperatureMin,
            BigDecimal temperatureMax,
            List<Waypoint> waypoints,
            List<RouteSegment> segments,
            BigDecimal totalDistanceKm,
            BigDecimal totalCost,
            Instant eta,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.orderNumber = Objects.requireNonNull(orderNumber, "orderNumber must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.cargoWeightKg = cargoWeightKg;
        this.cargoVolumeM3 = cargoVolumeM3;
        this.adrClass = adrClass;
        this.temperatureMin = temperatureMin;
        this.temperatureMax = temperatureMax;
        this.waypoints = List.copyOf(Objects.requireNonNull(waypoints, "waypoints must not be null"));
        this.segments = List.copyOf(Objects.requireNonNull(segments, "segments must not be null"));
        this.totalDistanceKm = totalDistanceKm;
        this.totalCost = totalCost;
        this.eta = eta;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static RouteOrder create(
            UUID id,
            String orderNumber,
            BigDecimal cargoWeightKg,
            BigDecimal cargoVolumeM3,
            AdrClass adrClass,
            BigDecimal temperatureMin,
            BigDecimal temperatureMax,
            List<Waypoint> waypoints,
            Instant createdAt
    ) {
        validateDraftWaypoints(waypoints);

        return new RouteOrder(
            id,
            orderNumber,
            RouteOrderStatus.DRAFT,
            cargoWeightKg,
            cargoVolumeM3,
            adrClass,
            temperatureMin,
            temperatureMax,
            waypoints,
            List.of(),
            null,
            null,
            null,
            createdAt,
            createdAt
        );
    }

    private static void validateDraftWaypoints(List<Waypoint> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new InvalidRouteDraftException("a route draft must contain at least two waypoints");
        }

        Set<Integer> sequenceNumbers = new HashSet<>();
        for (Waypoint waypoint : waypoints) {
            if (!sequenceNumbers.add(waypoint.getSequenceNumber())) {
                throw new InvalidRouteDraftException("waypoint sequence numbers must be unique");
            }
        }

        List<Waypoint> sortedWaypoints = waypoints.stream()
                .sorted(Comparator.comparingInt(Waypoint::getSequenceNumber))
                .toList();

        Waypoint firstWaypoint = sortedWaypoints.getFirst();
        Waypoint lastWaypoint = sortedWaypoints.getLast();

        if (firstWaypoint.getType() != WaypointType.ORIGIN) {
            throw new InvalidRouteDraftException("the first waypoint must be an ORIGIN");
        }

        if (lastWaypoint.getType() != WaypointType.DESTINATION) {
            throw new InvalidRouteDraftException("the last waypoint must be a DESTINATION");
        }
    }
}
