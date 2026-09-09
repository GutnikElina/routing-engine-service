package com.logistics.routing.adapter.out.osrm;

import com.logistics.routing.application.routing.GeoCoordinate;
import com.logistics.routing.domain.route.exception.RoutingEngineException;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;

@UtilityClass
public class OsrmCoordinateValidator {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    public void validateForMatrix(List<GeoCoordinate> coordinates, int maxCoordinates) {
        validateNotEmpty(coordinates);
        validateMaxSize(coordinates, maxCoordinates);
        coordinates.forEach(OsrmCoordinateValidator::validateCoordinate);
    }

    public void validateForRoute(List<GeoCoordinate> coordinates, int maxCoordinates) {
        validateForMatrix(coordinates, maxCoordinates);
        if (coordinates.size() < 2) {
            throw new RoutingEngineException("Route requires at least 2 waypoints");
        }
    }

    private void validateNotEmpty(List<GeoCoordinate> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            throw new RoutingEngineException("At least one coordinate is required");
        }
    }

    private void validateMaxSize(List<GeoCoordinate> coordinates, int maxCoordinates) {
        if (coordinates.size() > maxCoordinates) {
            throw new RoutingEngineException(
                    "Coordinate count " + coordinates.size() + " exceeds maximum allowed " + maxCoordinates
            );
        }
    }

    private void validateCoordinate(GeoCoordinate coordinate) {
        if (coordinate == null) {
            throw new RoutingEngineException("Coordinate must not be null");
        }

        validateAxis(coordinate.latitude(), "Latitude", MIN_LATITUDE, MAX_LATITUDE);
        validateAxis(coordinate.longitude(), "Longitude", MIN_LONGITUDE, MAX_LONGITUDE);
    }

    private void validateAxis(BigDecimal value, String name, BigDecimal min, BigDecimal max) {
        if (value == null) {
            throw new RoutingEngineException(name + " must not be null");
        }

        if (!Double.isFinite(value.doubleValue())) {
            throw new RoutingEngineException("Coordinates must be finite numbers");
        }
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new RoutingEngineException(name + " must be between " + min + " and " + max);
        }
    }
}
