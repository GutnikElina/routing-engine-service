package com.logistics.routing.application.port.out;

import com.logistics.routing.application.routing.DistanceMatrix;
import com.logistics.routing.application.routing.GeoCoordinate;
import com.logistics.routing.application.routing.RouteGeometry;
import com.logistics.routing.domain.route.model.enums.TransportType;

import java.util.List;

public interface RoutingEnginePort {

    DistanceMatrix getDistanceMatrix(TransportType transportType, List<GeoCoordinate> coordinates);

    RouteGeometry getRouteGeometry(TransportType transportType, List<GeoCoordinate> waypointsInOrder);
}
