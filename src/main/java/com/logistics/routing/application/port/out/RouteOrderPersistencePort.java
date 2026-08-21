package com.logistics.routing.application.port.out;

import com.logistics.routing.domain.route.model.RouteOrder;

public interface RouteOrderPersistencePort {
    void save(RouteOrder routeOrder);
}
