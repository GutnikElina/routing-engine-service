package com.logistics.routing.adapter.out.osrm;

import com.logistics.routing.adapter.out.osrm.dto.OsrmRouteResponse;
import com.logistics.routing.adapter.out.osrm.dto.OsrmTableResponse;
import com.logistics.routing.application.routing.DistanceMatrix;
import com.logistics.routing.application.routing.GeoCoordinate;
import com.logistics.routing.application.routing.RouteGeometry;
import com.logistics.routing.domain.route.exception.RoutingEngineException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Slf4j
@UtilityClass
public class OsrmResponseMapper {

    private static final int LARGE_GEOMETRY_WARN_THRESHOLD = 100_000;

    public DistanceMatrix toDistanceMatrix(OsrmTableResponse response) {
        assertOkResponse(response.code(), response.message());

        if (response.durations() == null || response.distances() == null) {
            throw new RoutingEngineException("OSRM table response is missing durations or distances");
        }
        assertMatrixHasNoNulls(response.durations(), "durations");
        assertMatrixHasNoNulls(response.distances(), "distances");

        return new DistanceMatrix(
                OsrmNumberConverter.toBigDecimalMatrix(response.durations()),
                OsrmNumberConverter.toBigDecimalMatrix(response.distances())
        );
    }

    public RouteGeometry toRouteGeometry(OsrmRouteResponse response) {
        assertOkResponse(response.code(), response.message());

        if (response.routes() == null || response.routes().isEmpty()) {
            throw new RoutingEngineException("OSRM route response does not contain any routes");
        }

        OsrmRouteResponse.Route route = response.routes().getFirst();
        if (route.geometry() == null
                || route.geometry().coordinates() == null
                || route.geometry().coordinates().isEmpty()) {
            throw new RoutingEngineException("OSRM route response does not contain geometry");
        }

        List<List<Double>> coordinates = route.geometry().coordinates();
        if (coordinates.size() > LARGE_GEOMETRY_WARN_THRESHOLD) {
            log.atWarn().log("OSRM returned unusually large route geometry with {} points", coordinates.size());
        }

        List<GeoCoordinate> path = coordinates.stream()
                .map(OsrmResponseMapper::toGeoCoordinate)
                .toList();

        return new RouteGeometry(
                path,
                toBigDecimalOrZero(route.distance()),
                toBigDecimalOrZero(route.duration())
        );
    }

    private GeoCoordinate toGeoCoordinate(List<Double> coordinate) {
        if (coordinate == null || coordinate.size() < 2) {
            throw new RoutingEngineException("OSRM geometry coordinate must contain longitude and latitude");
        }

        return new GeoCoordinate(
                BigDecimal.valueOf(coordinate.get(1)),
                BigDecimal.valueOf(coordinate.get(0))
        );
    }

    private BigDecimal toBigDecimalOrZero(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private void assertMatrixHasNoNulls(List<List<Double>> matrix, String matrixName) {
        if (matrix.stream().anyMatch(row -> row == null || row.stream().anyMatch(Objects::isNull))) {
            throw new RoutingEngineException("OSRM table response contains null values in " + matrixName);
        }
    }

    private void assertOkResponse(String code, String message) {
        if ("Ok".equals(code)) {
            return;
        }

        String errorMessage = "OSRM returned code: " + code;
        if (message != null && !message.isBlank()) {
            errorMessage += " - " + message;
        }
        throw new RoutingEngineException(errorMessage);
    }
}
