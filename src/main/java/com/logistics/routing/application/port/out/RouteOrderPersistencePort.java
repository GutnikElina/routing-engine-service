package com.logistics.routing.application.port.out;

import com.logistics.routing.domain.route.model.RouteOrder;

import java.util.UUID;

public interface RouteOrderPersistencePort {
    UUID save(RouteOrder routeOrder);
}
