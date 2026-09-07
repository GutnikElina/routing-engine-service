package com.logistics.routing.unit.vrp;

import com.logistics.routing.adapter.out.ortools.OrToolsVrpSolverAdapter;
import com.logistics.routing.domain.vrp.exception.VrpSolverException;
import com.logistics.routing.domain.vrp.model.VrpProblem;
import com.logistics.routing.domain.vrp.model.VrpSolution;
import com.logistics.routing.domain.vrp.model.VrpStop;
import com.logistics.routing.domain.vrp.model.VrpVehicle;
import com.logistics.routing.domain.vrp.model.VrpVehicleRoute;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrToolsVrpSolverAdapterTest {

    private static final Instant PLANNING_START = Instant.parse("2026-01-01T08:00:00Z");

    // 4 locations (0 = depot); distances in meters, travel at ~36 km/h (10 m/s).
    private static final List<List<BigDecimal>> DISTANCE_METERS = matrix(new long[][]{
            {0, 100, 200, 300},
            {100, 0, 100, 200},
            {200, 100, 0, 100},
            {300, 200, 100, 0}
    });
    private static final List<List<BigDecimal>> DURATION_SECONDS = matrix(new long[][]{
            {0, 10, 20, 30},
            {10, 0, 10, 20},
            {20, 10, 0, 10},
            {30, 20, 10, 0}
    });

    private final OrToolsVrpSolverAdapter adapter = new OrToolsVrpSolverAdapter();

    @Test
    void solvesSingleVehicleRouteVisitingAllStops() {
        VrpProblem problem = new VrpProblem(
                DISTANCE_METERS,
                DURATION_SECONDS,
                List.of(
                        VrpStop.depot(0),
                        stop(1, "100", "1", null, null),
                        stop(2, "100", "1", null, null),
                        stop(3, "100", "1", null, null)
                ),
                List.of(vehicle("truck-1", "1000", "10")),
                PLANNING_START,
                Duration.ZERO,
                Duration.ZERO,
                Duration.ofSeconds(5)
        );

        VrpSolution solution = adapter.solve(problem);

        assertThat(solution.routes()).hasSize(1);
        assertThat(solution.unassignedStopIndices()).isEmpty();
        VrpVehicleRoute route = solution.routes().getFirst();
        assertThat(route.stops()).hasSize(5);
        assertThat(route.stops().getFirst().locationIndex()).isZero();
        assertThat(route.stops().getLast().locationIndex()).isZero();
        assertThat(route.stops().stream().map(s -> s.locationIndex()).distinct().toList()).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @Test
    void splitsStopsAcrossVehiclesWhenCapacityIsExceeded() {
        VrpProblem problem = new VrpProblem(
                DISTANCE_METERS,
                DURATION_SECONDS,
                List.of(
                        VrpStop.depot(0),
                        stop(1, "100", "1", null, null),
                        stop(2, "100", "1", null, null),
                        stop(3, "100", "1", null, null)
                ),
                List.of(vehicle("truck-1", "250", "10"), vehicle("truck-2", "250", "10")),
                PLANNING_START,
                Duration.ZERO,
                Duration.ZERO,
                Duration.ofSeconds(5)
        );

        VrpSolution solution = adapter.solve(problem);

        assertThat(solution.routes()).hasSize(2);
        for (VrpVehicleRoute route : solution.routes()) {
            assertThat(route.totalWeightKg()).isLessThanOrEqualTo(new BigDecimal("250"));
        }
        long distinctStopsServed = solution.routes().stream()
                .flatMap(route -> route.stops().stream())
                .map(s -> s.locationIndex())
                .distinct()
                .count();
        assertThat(distinctStopsServed).isEqualTo(4); // depot + 3 customer stops
    }

    @Test
    void respectsPerStopTimeWindow() {
        VrpProblem problem = new VrpProblem(
                DISTANCE_METERS,
                DURATION_SECONDS,
                List.of(
                        VrpStop.depot(0),
                        stop(1, "100", "1", PLANNING_START.plusSeconds(50), PLANNING_START.plusSeconds(60)),
                        stop(2, "100", "1", null, null),
                        stop(3, "100", "1", null, null)
                ),
                List.of(vehicle("truck-1", "1000", "10")),
                PLANNING_START,
                Duration.ZERO,
                Duration.ZERO,
                Duration.ofSeconds(5)
        );

        VrpSolution solution = adapter.solve(problem);

        VrpVehicleRoute route = solution.routes().getFirst();
        Instant arrivalAtStop1 = route.stops().stream()
                .filter(s -> s.locationIndex() == 1)
                .findFirst()
                .orElseThrow()
                .arrivalTime();

        assertThat(arrivalAtStop1).isBetween(
                PLANNING_START.plusSeconds(50),
                PLANNING_START.plusSeconds(60)
        );
    }

    @Test
    void throwsWhenNoVehicleCanCoverTotalDemand() {
        VrpProblem problem = new VrpProblem(
                DISTANCE_METERS,
                DURATION_SECONDS,
                List.of(
                        VrpStop.depot(0),
                        stop(1, "10000", "1", null, null),
                        stop(2, "100", "1", null, null),
                        stop(3, "100", "1", null, null)
                ),
                List.of(vehicle("truck-1", "10", "10")),
                PLANNING_START,
                Duration.ZERO,
                Duration.ZERO,
                Duration.ofSeconds(5)
        );

        assertThatThrownBy(() -> adapter.solve(problem))
                .isInstanceOf(VrpSolverException.class);
    }

    @Test
    void solvesWhenMandatoryDriverBreakIsRequired() {
        VrpProblem problem = new VrpProblem(
                DISTANCE_METERS,
                DURATION_SECONDS,
                List.of(
                        VrpStop.depot(0),
                        stop(1, "100", "1", null, null),
                        stop(2, "100", "1", null, null),
                        stop(3, "100", "1", null, null)
                ),
                List.of(vehicle("truck-1", "1000", "10")),
                PLANNING_START,
                Duration.ofSeconds(5),
                Duration.ofSeconds(15),
                Duration.ofSeconds(5)
        );

        VrpSolution solution = adapter.solve(problem);

        assertThat(solution.routes()).hasSize(1);
        assertThat(solution.unassignedStopIndices()).isEmpty();
    }

    private static VrpStop stop(int index, String weightKg, String volumeM3, Instant windowStart, Instant windowEnd) {
        return new VrpStop(index, new BigDecimal(weightKg), new BigDecimal(volumeM3), windowStart, windowEnd, Duration.ZERO);
    }

    private static VrpVehicle vehicle(String id, String capacityWeightKg, String capacityVolumeM3) {
        return new VrpVehicle(id, new BigDecimal(capacityWeightKg), new BigDecimal(capacityVolumeM3));
    }

    private static List<List<BigDecimal>> matrix(long[][] values) {
        return java.util.Arrays.stream(values)
                .map(row -> java.util.Arrays.stream(row).mapToObj(BigDecimal::valueOf).toList())
                .toList();
    }
}
