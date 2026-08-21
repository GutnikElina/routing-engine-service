package com.logistics.routing.adapter.out.persistence;

public enum RouteOrderStatus {
    DRAFT,
    PLANNED,
    READY_TO_START,
    BLOCKED,
    IN_TRANSIT,
    NEEDS_REPLANNING,
    DELIVERED,
    FAILED,
    CLOSED
}