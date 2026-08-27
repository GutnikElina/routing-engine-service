package com.logistics.routing.domain.route.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.logistics.routing.domain.route.exception.InvalidRouteDraftException;
import com.logistics.routing.domain.route.model.enums.WaypointType;

@Getter
public class Waypoint {
    private final UUID id;
    private final WaypointType type;
    private final Integer sequenceNumber;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String address;
    private final Instant timeWindowStart;
    private final Instant timeWindowEnd;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Waypoint(
            UUID id,
            WaypointType type,
            Integer sequenceNumber,
            BigDecimal latitude,
            BigDecimal longitude,
            String address,
            Instant timeWindowStart,
            Instant timeWindowEnd,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.sequenceNumber = Objects.requireNonNull(sequenceNumber, "sequenceNumber must not be null");
        this.latitude = Objects.requireNonNull(latitude, "latitude must not be null");
        this.longitude = Objects.requireNonNull(longitude, "longitude must not be null");
        this.address = address;
        validateTimeWindow(timeWindowStart, timeWindowEnd);
        this.timeWindowStart = timeWindowStart;
        this.timeWindowEnd = timeWindowEnd;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    private static void validateTimeWindow(Instant start, Instant end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidRouteDraftException(
                    "timeWindowStart must be before or equal to timeWindowEnd"
            );
        }
    }
}
