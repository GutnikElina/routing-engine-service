package com.logistics.routing.adapter.out.straightline;

import com.logistics.routing.adapter.out.routing.RoutingClient;
import com.logistics.routing.application.routing.DistanceMatrix;
import com.logistics.routing.application.routing.GeoCoordinate;
import com.logistics.routing.application.routing.RouteGeometry;
import com.logistics.routing.domain.route.exception.RoutingEngineException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class StraightLineRoutingClient implements RoutingClient {

    private static final int INTERPOLATION_STEPS_PER_LEG = 16;

    private final BigDecimal averageSpeedKmh;

    public StraightLineRoutingClient(BigDecimal averageSpeedKmh) {
        if (averageSpeedKmh == null || averageSpeedKmh.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Average speed must be greater than zero");
        }
        this.averageSpeedKmh = averageSpeedKmh;
    }

    @Override
    public DistanceMatrix getDistanceMatrix(List<GeoCoordinate> coordinates) {
        int size = coordinates.size();
        List<List<BigDecimal>> durations = new ArrayList<>(size);
        List<List<BigDecimal>> distances = new ArrayList<>(size);

        for (int sourceIndex = 0; sourceIndex < size; sourceIndex++) {
            List<BigDecimal> durationRow = new ArrayList<>(size);
            List<BigDecimal> distanceRow = new ArrayList<>(size);

            for (int destinationIndex = 0; destinationIndex < size; destinationIndex++) {
                if (sourceIndex == destinationIndex) {
                    durationRow.add(BigDecimal.ZERO);
                    distanceRow.add(BigDecimal.ZERO);
                    continue;
                }

                BigDecimal distanceMeters = StraightLineCalculator.distanceMeters(
                        coordinates.get(sourceIndex),
                        coordinates.get(destinationIndex)
                );
                durationRow.add(StraightLineCalculator.durationSeconds(distanceMeters, averageSpeedKmh));
                distanceRow.add(distanceMeters);
            }

            durations.add(durationRow);
            distances.add(distanceRow);
        }

        return new DistanceMatrix(durations, distances);
    }

    @Override
    public RouteGeometry getRouteGeometry(List<GeoCoordinate> waypointsInOrder) {
        if (waypointsInOrder.size() < 2) {
            throw new RoutingEngineException("Route requires at least 2 waypoints");
        }

        List<GeoCoordinate> path = buildPath(waypointsInOrder);
        BigDecimal totalDistanceMeters = BigDecimal.ZERO;
        BigDecimal totalDurationSeconds = BigDecimal.ZERO;

        for (int index = 1; index < waypointsInOrder.size(); index++) {
            GeoCoordinate from = waypointsInOrder.get(index - 1);
            GeoCoordinate to = waypointsInOrder.get(index);
            BigDecimal legDistanceMeters = StraightLineCalculator.distanceMeters(from, to);
            totalDistanceMeters = totalDistanceMeters.add(legDistanceMeters);
            totalDurationSeconds = totalDurationSeconds.add(
                    StraightLineCalculator.durationSeconds(legDistanceMeters, averageSpeedKmh)
            );
        }

        return new RouteGeometry(path, totalDistanceMeters, totalDurationSeconds);
    }

    private List<GeoCoordinate> buildPath(List<GeoCoordinate> waypointsInOrder) {
        List<GeoCoordinate> path = new ArrayList<>();
        path.add(waypointsInOrder.getFirst());

        for (int legIndex = 1; legIndex < waypointsInOrder.size(); legIndex++) {
            GeoCoordinate from = waypointsInOrder.get(legIndex - 1);
            GeoCoordinate to = waypointsInOrder.get(legIndex);

            for (int step = 1; step <= INTERPOLATION_STEPS_PER_LEG; step++) {
                BigDecimal fraction = BigDecimal.valueOf(step)
                        .divide(BigDecimal.valueOf(INTERPOLATION_STEPS_PER_LEG), 8, RoundingMode.HALF_UP);
                path.add(StraightLineCalculator.interpolate(from, to, fraction));
            }
        }

        return path;
    }
}
