package com.logistics.routing.adapter.out.ortools;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.IntervalVar;
import com.google.ortools.constraintsolver.LocalSearchMetaheuristic;
import com.google.ortools.constraintsolver.RoutingDimension;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import com.google.ortools.constraintsolver.main;
import com.logistics.routing.application.port.out.VrpSolverPort;
import com.logistics.routing.domain.vrp.exception.VrpSolverException;
import com.logistics.routing.domain.vrp.model.VrpProblem;
import com.logistics.routing.domain.vrp.model.VrpRouteStop;
import com.logistics.routing.domain.vrp.model.VrpSolution;
import com.logistics.routing.domain.vrp.model.VrpStop;
import com.logistics.routing.domain.vrp.model.VrpVehicle;
import com.logistics.routing.domain.vrp.model.VrpVehicleRoute;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Solves a {@link VrpProblem} with Google OR-Tools' constraint-programming routing solver
 * (CVRPTW: capacity on weight and volume, per-stop time windows, one mandatory driver break
 * per vehicle once {@link VrpProblem#maxDrivingDurationBeforeBreak()} is exceeded).
 */
@Component
public class OrToolsVrpSolverAdapter implements VrpSolverPort {

    private static final int DEPOT_INDEX = VrpProblem.DEPOT_INDEX;
    private static final String CAPACITY_WEIGHT_DIMENSION = "CapacityWeight";
    private static final String CAPACITY_VOLUME_DIMENSION = "CapacityVolume";
    private static final String TIME_DIMENSION = "Time";

    // m3 -> liters, so demand/capacity keep 3 decimal places of precision as OR-Tools longs.
    private static final int VOLUME_SCALE = 1000;
    private static final int WEIGHT_SCALE = 1;

    static {
        Loader.loadNativeLibraries();
    }

    @Override
    public VrpSolution solve(VrpProblem problem) {
        int vehicleCount = problem.vehicles().size();

        RoutingIndexManager manager =
                new RoutingIndexManager(problem.stops().size(), vehicleCount, DEPOT_INDEX);
        RoutingModel routing = new RoutingModel(manager);

        long[][] distanceMatrix = toLongMatrix(problem.distanceMatrixMeters());
        long[][] durationMatrix = toLongMatrix(problem.durationMatrixSeconds());

        int distanceCallbackIndex = registerDistanceCallback(routing, manager, distanceMatrix);
        routing.setArcCostEvaluatorOfAllVehicles(distanceCallbackIndex);

        addCapacityDimension(routing, manager, problem, CAPACITY_WEIGHT_DIMENSION,
                VrpStop::demandWeightKg, VrpVehicle::capacityWeightKg, WEIGHT_SCALE);
        addCapacityDimension(routing, manager, problem, CAPACITY_VOLUME_DIMENSION,
                VrpStop::demandVolumeM3, VrpVehicle::capacityVolumeM3, VOLUME_SCALE);

        long horizonSeconds = computeHorizonSeconds(problem, durationMatrix);
        addTimeDimension(routing, manager, problem, durationMatrix, horizonSeconds);
        RoutingDimension timeDimension = routing.getMutableDimension(TIME_DIMENSION);
        applyTimeWindows(routing, manager, timeDimension, problem, vehicleCount, horizonSeconds);
        addDriverBreaks(routing, manager, timeDimension, problem, vehicleCount, horizonSeconds);

        RoutingSearchParameters searchParameters = main.defaultRoutingSearchParameters().toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                .setTimeLimit(com.google.protobuf.Duration.newBuilder()
                        .setSeconds(problem.solverTimeLimit().toSeconds())
                        .build())
                .build();

        Assignment assignment = routing.solveWithParameters(searchParameters);
        if (assignment == null) {
            throw new VrpSolverException("no feasible VRP solution found for the given constraints");
        }

        return buildSolution(routing, manager, assignment, problem, vehicleCount, timeDimension);
    }

    private int registerDistanceCallback(RoutingModel routing, RoutingIndexManager manager, long[][] distanceMatrix) {
        return routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            int toNode = manager.indexToNode(toIndex);
            return distanceMatrix[fromNode][toNode];
        });
    }

    private void addCapacityDimension(
            RoutingModel routing,
            RoutingIndexManager manager,
            VrpProblem problem,
            String dimensionName,
            Function<VrpStop, BigDecimal> demandExtractor,
            Function<VrpVehicle, BigDecimal> capacityExtractor,
            int scale
    ) {
        long[] demands = problem.stops().stream()
                .mapToLong(stop -> scale(demandExtractor.apply(stop), scale, RoundingMode.CEILING))
                .toArray();

        int demandCallbackIndex = routing.registerUnaryTransitCallback((long fromIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            return demands[fromNode];
        });

        long[] vehicleCapacities = problem.vehicles().stream()
                .mapToLong(vehicle -> scale(capacityExtractor.apply(vehicle), scale, RoundingMode.FLOOR))
                .toArray();

        routing.addDimensionWithVehicleCapacity(demandCallbackIndex, 0, vehicleCapacities, true, dimensionName);
    }

    private void addTimeDimension(
            RoutingModel routing,
            RoutingIndexManager manager,
            VrpProblem problem,
            long[][] durationMatrix,
            long horizonSeconds
    ) {
        long[] serviceDurations = problem.stops().stream()
                .mapToLong(stop -> stop.serviceDuration().toSeconds())
                .toArray();

        int timeCallbackIndex = routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            int toNode = manager.indexToNode(toIndex);
            return durationMatrix[fromNode][toNode] + serviceDurations[fromNode];
        });

        routing.addDimension(timeCallbackIndex, horizonSeconds, horizonSeconds, true, TIME_DIMENSION);
    }

    private void applyTimeWindows(
            RoutingModel routing,
            RoutingIndexManager manager,
            RoutingDimension timeDimension,
            VrpProblem problem,
            int vehicleCount,
            long horizonSeconds
    ) {
        List<VrpStop> stops = problem.stops();
        for (int nodeIndex = 0; nodeIndex < stops.size(); nodeIndex++) {
            if (nodeIndex == DEPOT_INDEX) {
                continue;
            }
            VrpStop stop = stops.get(nodeIndex);
            long start = toRelativeSeconds(problem.planningStartTime(), stop.timeWindowStart(), 0);
            long end = toRelativeSeconds(problem.planningStartTime(), stop.timeWindowEnd(), horizonSeconds);
            long index = manager.nodeToIndex(nodeIndex);
            timeDimension.cumulVar(index).setRange(start, end);
        }

        for (int vehicleIndex = 0; vehicleIndex < vehicleCount; vehicleIndex++) {
            routing.addVariableMinimizedByFinalizer(timeDimension.cumulVar(routing.start(vehicleIndex)));
            routing.addVariableMinimizedByFinalizer(timeDimension.cumulVar(routing.end(vehicleIndex)));
        }
    }

    /**
     * Adds a single mandatory driver rest per vehicle once cumulative driving/service time
     * exceeds {@code maxDrivingDurationBeforeBreak}. Longer, multi-shift horizons that would
     * require repeated rests are out of scope for this classical VRP adapter.
     */
    private void addDriverBreaks(
            RoutingModel routing,
            RoutingIndexManager manager,
            RoutingDimension timeDimension,
            VrpProblem problem,
            int vehicleCount,
            long horizonSeconds
    ) {
        long maxDrivingSeconds = problem.maxDrivingDurationBeforeBreak().toSeconds();
        long breakSeconds = problem.breakDuration().toSeconds();
        if (maxDrivingSeconds <= 0 || breakSeconds <= 0 || horizonSeconds <= maxDrivingSeconds + breakSeconds) {
            return;
        }

        long[] nodeVisitTransit = new long[(int) routing.size()];
        for (int index = 0; index < nodeVisitTransit.length; index++) {
            int node = manager.indexToNode(index);
            nodeVisitTransit[index] = problem.stops().get(node).serviceDuration().toSeconds();
        }

        long latestStart = horizonSeconds - breakSeconds;
        for (int vehicleIndex = 0; vehicleIndex < vehicleCount; vehicleIndex++) {
            IntervalVar breakInterval = routing.solver().makeFixedDurationIntervalVar(
                    maxDrivingSeconds,
                    latestStart,
                    breakSeconds,
                    false,
                    "vehicle-" + vehicleIndex + "-break"
            );
            timeDimension.setBreakIntervalsOfVehicle(
                    new IntervalVar[] {breakInterval}, vehicleIndex, nodeVisitTransit);
        }
    }

    private VrpSolution buildSolution(
            RoutingModel routing,
            RoutingIndexManager manager,
            Assignment assignment,
            VrpProblem problem,
            int vehicleCount,
            RoutingDimension timeDimension
    ) {
        List<VrpVehicleRoute> routes = new ArrayList<>();
        boolean[] visited = new boolean[problem.stops().size()];
        BigDecimal totalDistance = BigDecimal.ZERO;
        long totalDurationSeconds = 0;

        for (int vehicleIndex = 0; vehicleIndex < vehicleCount; vehicleIndex++) {
            long index = routing.start(vehicleIndex);
            long routeStartSeconds = assignment.min(timeDimension.cumulVar(index));
            List<VrpRouteStop> routeStops = new ArrayList<>();
            BigDecimal routeDistance = BigDecimal.ZERO;
            BigDecimal routeWeight = BigDecimal.ZERO;
            BigDecimal routeVolume = BigDecimal.ZERO;

            while (!routing.isEnd(index)) {
                int node = manager.indexToNode(index);
                visited[node] = true;
                VrpStop stop = problem.stops().get(node);
                Instant arrival = problem.planningStartTime().plusSeconds(assignment.min(timeDimension.cumulVar(index)));
                routeStops.add(new VrpRouteStop(node, arrival, arrival.plus(stop.serviceDuration())));
                routeWeight = routeWeight.add(stop.demandWeightKg());
                routeVolume = routeVolume.add(stop.demandVolumeM3());

                long previousIndex = index;
                index = assignment.value(routing.nextVar(index));
                int fromNode = manager.indexToNode(previousIndex);
                int toNode = manager.indexToNode(index);
                routeDistance = routeDistance.add(problem.distanceMatrixMeters().get(fromNode).get(toNode));
            }

            int endNode = manager.indexToNode(index);
            visited[endNode] = true;
            long endArrivalSeconds = assignment.min(timeDimension.cumulVar(index));
            Instant endArrival = problem.planningStartTime().plusSeconds(endArrivalSeconds);
            routeStops.add(new VrpRouteStop(endNode, endArrival, endArrival));

            if (routeStops.size() > 2) {
                long routeDurationSeconds = endArrivalSeconds - routeStartSeconds;
                routes.add(new VrpVehicleRoute(
                        problem.vehicles().get(vehicleIndex).id(),
                        routeStops,
                        routeDistance,
                        Duration.ofSeconds(routeDurationSeconds),
                        routeWeight,
                        routeVolume
                ));
                totalDistance = totalDistance.add(routeDistance);
                totalDurationSeconds += routeDurationSeconds;
            }
        }

        List<Integer> unassigned = new ArrayList<>();
        for (int node = 0; node < problem.stops().size(); node++) {
            if (node != DEPOT_INDEX && !visited[node]) {
                unassigned.add(node);
            }
        }

        return new VrpSolution(routes, unassigned, totalDistance, Duration.ofSeconds(totalDurationSeconds));
    }

    private long computeHorizonSeconds(VrpProblem problem, long[][] durationMatrix) {
        long latestWindowEnd = 0;
        for (VrpStop stop : problem.stops()) {
            if (stop.timeWindowEnd() != null) {
                latestWindowEnd = Math.max(latestWindowEnd,
                        toRelativeSeconds(problem.planningStartTime(), stop.timeWindowEnd(), 0));
            }
        }

        long maxLegDuration = 0;
        for (long[] row : durationMatrix) {
            for (long value : row) {
                maxLegDuration = Math.max(maxLegDuration, value);
            }
        }
        long fallbackHorizon = maxLegDuration * problem.stops().size()
                + problem.breakDuration().toSeconds() * problem.vehicles().size();

        return Math.max(latestWindowEnd, fallbackHorizon) + problem.breakDuration().toSeconds() + 1;
    }

    private long toRelativeSeconds(Instant planningStart, Instant instant, long fallback) {
        if (instant == null) {
            return fallback;
        }
        return Math.max(0, Duration.between(planningStart, instant).toSeconds());
    }

    private long scale(BigDecimal value, int scale, RoundingMode roundingMode) {
        return value.multiply(BigDecimal.valueOf(scale)).setScale(0, roundingMode).longValueExact();
    }

    private long[][] toLongMatrix(List<List<BigDecimal>> matrix) {
        long[][] result = new long[matrix.size()][];
        for (int i = 0; i < matrix.size(); i++) {
            List<BigDecimal> row = matrix.get(i);
            result[i] = new long[row.size()];
            for (int j = 0; j < row.size(); j++) {
                result[i][j] = row.get(j).setScale(0, RoundingMode.HALF_UP).longValueExact();
            }
        }
        return result;
    }
}
