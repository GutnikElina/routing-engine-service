package com.logistics.routing.domain.vrp.model;

import java.math.BigDecimal;
import java.util.Objects;

public record VrpVehicle(
        String id,
        BigDecimal capacityWeightKg,
        BigDecimal capacityVolumeM3
) {

    public VrpVehicle {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(capacityWeightKg, "capacityWeightKg must not be null");
        Objects.requireNonNull(capacityVolumeM3, "capacityVolumeM3 must not be null");
        if (capacityWeightKg.signum() < 0 || capacityVolumeM3.signum() < 0) {
            throw new IllegalArgumentException("vehicle capacities must not be negative");
        }
    }
}
