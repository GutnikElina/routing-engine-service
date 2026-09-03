package com.logistics.routing.adapter.out.osrm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmTableResponse(
        String code,
        String message,
        List<List<Double>> durations,
        List<List<Double>> distances
) {
}
