package com.logistics.routing.domain.vrp.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public record VrpStop(
        int locationIndex,
        BigDecimal demandWeightKg,
        BigDecimal demandVolumeM3,
        Instant timeWindowStart,
        Instant timeWindowEnd,
        Duration serviceDuration
) {

    public VrpStop {
        if (locationIndex < 0) {
            throw new IllegalArgumentException("locationIndex must not be negative");
        }
        demandWeightKg = demandWeightKg == null ? BigDecimal.ZERO : demandWeightKg;
        demandVolumeM3 = demandVolumeM3 == null ? BigDecimal.ZERO : demandVolumeM3;
        serviceDuration = serviceDuration == null ? Duration.ZERO : serviceDuration;
        if (demandWeightKg.signum() < 0 || demandVolumeM3.signum() < 0) {
            throw new IllegalArgumentException("stop demand must not be negative");
        }
        if (timeWindowStart != null && timeWindowEnd != null && timeWindowStart.isAfter(timeWindowEnd)) {
            throw new IllegalArgumentException("timeWindowStart must not be after timeWindowEnd");
        }
    }

    public static VrpStop depot(int locationIndex) {
        return new VrpStop(locationIndex, BigDecimal.ZERO, BigDecimal.ZERO, null, null, Duration.ZERO);
    }
}
