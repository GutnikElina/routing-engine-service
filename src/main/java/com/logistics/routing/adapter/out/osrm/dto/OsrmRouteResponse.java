package com.logistics.routing.adapter.out.osrm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmRouteResponse(
        String code,
        String message,
        List<Route> routes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
            Geometry geometry,
            Double distance,
            Double duration
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometry(
            String type,
            List<List<Double>> coordinates
    ) {
    }
}
