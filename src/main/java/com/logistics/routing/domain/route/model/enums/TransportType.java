package com.logistics.routing.domain.route.model.enums;

public enum TransportType {
    TRUCK("TRUCK"),
    TRAIN("TRAIN"),
    VESSEL("VESSEL"),
    PLANE("PLANE");

    private final String code;

    TransportType(String code) {
        this.code = code;
    }
}
