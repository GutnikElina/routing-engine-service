package com.logistics.routing.domain.vrp.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Classical capacitated VRP with time windows: a single depot (stop index 0, no demand),
 * one or more vehicles bounded by weight/volume capacity, and per-stop time windows.
 * {@code maxDrivingDurationBeforeBreak}/{@code breakDuration} model a single mandatory
 * driver rest per vehicle route; leave them at {@link Duration#ZERO} to disable that rule.
 */
public record VrpProblem(
        List<List<BigDecimal>> distanceMatrixMeters,
        List<List<BigDecimal>> durationMatrixSeconds,
        List<VrpStop> stops,
        List<VrpVehicle> vehicles,
        Instant planningStartTime,
        Duration maxDrivingDurationBeforeBreak,
        Duration breakDuration,
        Duration solverTimeLimit
) {

    public static final int DEPOT_INDEX = 0;

    public VrpProblem {
        Objects.requireNonNull(distanceMatrixMeters, "distanceMatrixMeters must not be null");
        Objects.requireNonNull(durationMatrixSeconds, "durationMatrixSeconds must not be null");
        Objects.requireNonNull(stops, "stops must not be null");
        Objects.requireNonNull(vehicles, "vehicles must not be null");
        Objects.requireNonNull(planningStartTime, "planningStartTime must not be null");

        stops = List.copyOf(stops);
        vehicles = List.copyOf(vehicles);

        int size = stops.size();
        if (size < 2) {
            throw new IllegalArgumentException("a VRP problem requires the depot plus at least one stop");
        }
        if (vehicles.isEmpty()) {
            throw new IllegalArgumentException("a VRP problem requires at least one vehicle");
        }
        requireSquareMatrix(distanceMatrixMeters, size, "distanceMatrixMeters");
        requireSquareMatrix(durationMatrixSeconds, size, "durationMatrixSeconds");

        VrpStop depot = stops.get(DEPOT_INDEX);
        if (depot.demandWeightKg().signum() != 0 || depot.demandVolumeM3().signum() != 0) {
            throw new IllegalArgumentException("the depot stop (index 0) must not carry demand");
        }

        maxDrivingDurationBeforeBreak =
                maxDrivingDurationBeforeBreak == null ? Duration.ZERO : maxDrivingDurationBeforeBreak;
        breakDuration = breakDuration == null ? Duration.ZERO : breakDuration;
        solverTimeLimit = solverTimeLimit == null ? Duration.ofSeconds(5) : solverTimeLimit;
    }

    private static void requireSquareMatrix(List<List<BigDecimal>> matrix, int size, String name) {
        if (matrix.size() != size) {
            throw new IllegalArgumentException(name + " must have one row per stop");
        }
        for (List<BigDecimal> row : matrix) {
            if (row.size() != size) {
                throw new IllegalArgumentException(name + " must be a square matrix");
            }
        }
    }
}
