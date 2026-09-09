package com.logistics.routing.adapter.out.osrm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmTableRequest(
        List<List<Double>> coordinates,
        List<String> annotations
) {
    public static OsrmTableRequest of(List<List<Double>> coordinates) {
        return new OsrmTableRequest(coordinates, List.of("distance", "duration"));
    }
}
