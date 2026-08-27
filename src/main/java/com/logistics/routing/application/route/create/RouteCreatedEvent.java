package com.logistics.routing.application.route.create;

import com.logistics.routing.domain.route.model.enums.RouteOrderStatus;

import java.time.Instant;
import java.util.UUID;

public record RouteCreatedEvent(
        UUID routeId,
        String orderNumber,
        RouteOrderStatus status,
        Instant occurredAt
) {
}
