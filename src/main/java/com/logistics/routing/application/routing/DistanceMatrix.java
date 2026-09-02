package com.logistics.routing.application.routing;

import java.math.BigDecimal;
import java.util.List;

public record DistanceMatrix(
        List<List<BigDecimal>> durationsSeconds,
        List<List<BigDecimal>> distancesMeters
) {
}
