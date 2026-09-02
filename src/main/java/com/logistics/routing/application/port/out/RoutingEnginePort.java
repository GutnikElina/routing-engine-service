package com.logistics.routing.application.port.out;

import com.logistics.routing.application.routing.DistanceMatrix;
import com.logistics.routing.application.routing.GeoCoordinate;
import com.logistics.routing.application.routing.RouteGeometry;

import java.util.List;

public interface RoutingEnginePort {

    DistanceMatrix getDistanceMatrix(List<GeoCoordinate> coordinates);

    RouteGeometry getRouteGeometry(List<GeoCoordinate> waypointsInOrder);
}
