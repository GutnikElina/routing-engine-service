package com.logistics.routing.adapter.in.web.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

import com.logistics.routing.domain.route.WaypointType;

public record WaypointRequest(
    @NotNull
    WaypointType type,

    @NotNull
    @Positive
    Integer sequence,

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    BigDecimal latitude,

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    BigDecimal longitude,

    @Size(max = 255)
    String address,

    Instant timeWindowStart,

    Instant timeWindowEnd
) {
}
