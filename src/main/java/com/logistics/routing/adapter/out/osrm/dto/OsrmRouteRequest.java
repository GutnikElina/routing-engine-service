package com.logistics.routing.adapter.out.osrm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmRouteRequest(
        List<List<Double>> coordinates,
        String overview,
        String geometries,
        boolean steps
) {
    public static OsrmRouteRequest of(List<List<Double>> coordinates, String overview) {
        return new OsrmRouteRequest(coordinates, overview, "geojson", false);
    }
}
