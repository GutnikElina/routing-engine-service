package com.logistics.routing.adapter.out.persistence.route;

import com.logistics.routing.application.port.out.RouteOrderPersistencePort;
import com.logistics.routing.domain.route.model.RouteOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RouteOrderPersistenceAdapter implements RouteOrderPersistencePort {

    private final RouteOrderRepository routeOrderRepository;
    private final RouteOrderPersistenceMapper routeOrderPersistenceMapper;

    @Override
    public void save(RouteOrder routeOrder) {
        routeOrderRepository.save(routeOrderPersistenceMapper.toEntity(routeOrder));
    }
}
