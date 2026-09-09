package com.logistics.routing.adapter.out.routing;

import com.logistics.routing.adapter.out.osrm.OsrmCoordinateValidator;
import com.logistics.routing.application.port.out.RoutingEnginePort;
import com.logistics.routing.application.routing.DistanceMatrix;
import com.logistics.routing.application.routing.GeoCoordinate;
import com.logistics.routing.application.routing.RouteGeometry;
import com.logistics.routing.domain.route.exception.RoutingEngineException;
import com.logistics.routing.domain.route.model.enums.TransportType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Primary
@RequiredArgsConstructor
public class RoutingEngineAdapter implements RoutingEnginePort {

    private final Map<TransportType, RoutingClient> routingClients;
    private final RoutingProperties properties;

    @Override
    public DistanceMatrix getDistanceMatrix(TransportType transportType, List<GeoCoordinate> coordinates) {
        OsrmCoordinateValidator.validateForMatrix(coordinates, properties.getMaxCoordinates());
        return resolveClient(transportType).getDistanceMatrix(coordinates);
    }

    @Override
    public RouteGeometry getRouteGeometry(TransportType transportType, List<GeoCoordinate> waypointsInOrder) {
        OsrmCoordinateValidator.validateForRoute(waypointsInOrder, properties.getMaxCoordinates());
        return resolveClient(transportType).getRouteGeometry(waypointsInOrder);
    }

    private RoutingClient resolveClient(TransportType transportType) {
        RoutingClient client = routingClients.get(transportType);
        if (client == null) {
            throw new RoutingEngineException("No routing engine configured for transport type: " + transportType);
        }
        return client;
    }
}
