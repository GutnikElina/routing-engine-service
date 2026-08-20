package com.logistics.routing.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CreateRouteResponse(
    UUID routeId,
    String orderNumber,
    String status,
    Instant createdAt
) {
}
