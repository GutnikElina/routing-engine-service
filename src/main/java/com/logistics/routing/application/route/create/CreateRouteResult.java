package com.logistics.routing.application.route.create;

import java.time.Instant;
import java.util.UUID;

public record CreateRouteResult(
    UUID routeId,
    String orderNumber,
    String status,
    Instant createdAt
) {
}
