package com.logistics.routing.adapter.out.persistence.route;

import com.logistics.routing.application.port.out.RouteOrderPersistencePort;
import com.logistics.routing.domain.route.model.RouteOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RouteOrderPersistenceAdapter implements RouteOrderPersistencePort {

    private final RouteOrderRepository routeOrderRepository;
    private final RouteOrderPersistenceMapper routeOrderPersistenceMapper;
    private final WaypointPersistenceMapper waypointPersistenceMapper;

    @Override
    public UUID save(RouteOrder routeOrder) {
        RouteOrderEntity routeOrderEntity = routeOrderPersistenceMapper.toEntity(routeOrder);
        routeOrder.getWaypoints().stream()
                .map(waypointPersistenceMapper::toEntity)
                .forEach(routeOrderEntity::addWaypoint);

        return routeOrderRepository.save(routeOrderEntity).getId();
    }
}
