package com.logistics.routing.testdata;

import com.logistics.routing.adapter.out.osrm.dto.OsrmRouteResponse;
import com.logistics.routing.adapter.out.osrm.dto.OsrmTableResponse;
import com.logistics.routing.application.routing.GeoCoordinate;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;

@UtilityClass
public class OsrmTestData {

    public static GeoCoordinate coordinate(double latitude, double longitude) {
        return new GeoCoordinate(
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude)
        );
    }

    public static OsrmTableResponse okTableResponse(List<List<Double>> durations, List<List<Double>> distances) {
        return new OsrmTableResponse("Ok", null, durations, distances);
    }

    public static OsrmRouteResponse okRouteResponse(List<List<Double>> coordinates, double distance, double duration) {
        return new OsrmRouteResponse("Ok", null, List.of(new OsrmRouteResponse.Route(
                new OsrmRouteResponse.Geometry("LineString", coordinates), distance, duration)));
    }
}
