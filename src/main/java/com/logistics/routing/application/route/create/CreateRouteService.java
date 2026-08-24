package com.logistics.routing.application.route.create;

import com.logistics.routing.application.port.out.RouteOrderPersistencePort;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import jakarta.transaction.Transactional;
import com.logistics.routing.domain.route.model.RouteOrder;
import com.logistics.routing.domain.route.model.Waypoint;
import com.logistics.routing.domain.route.model.enums.AdrClass;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CreateRouteService implements CreateRouteUseCase {
    private final RouteOrderPersistencePort routeOrderPersistencePort;
    private final Clock clock;

    @Override
    @Transactional
    public CreateRouteResult execute(CreateRouteCommand command) {
        Instant now = Instant.now(clock);
        RouteOrder routeOrder = toRouteOrder(command, now);
        routeOrderPersistencePort.save(routeOrder);
        return toCreateRouteResult(routeOrder);
    }

    private RouteOrder toRouteOrder(CreateRouteCommand command, Instant now) {
        CargoCommand cargo = command.cargo();
    
        List<Waypoint> waypoints = command.waypoints().stream()
                .map(waypoint -> toWaypoint(waypoint, now))
                .toList();
    
        AdrClass adrClass = cargo.adrClass() == null
                ? null
                : AdrClass.fromCode(cargo.adrClass());
    
        return RouteOrder.create(
                UUID.randomUUID(),
                command.orderNumber(),
                cargo.weightKg(),
                cargo.volumeM3(),
                adrClass,
                cargo.temperatureMin(),
                cargo.temperatureMax(),
                waypoints,
                now
        );
    }
    
    private Waypoint toWaypoint(WaypointCommand command, Instant now) {
        return new Waypoint(
                UUID.randomUUID(),
                command.type(),
                command.sequence(),
                command.latitude(),
                command.longitude(),
                command.address(),
                command.timeWindowStart(),
                command.timeWindowEnd(),
                now,
                now
        );
    }
    
    private CreateRouteResult toCreateRouteResult(RouteOrder routeOrder) {
        return new CreateRouteResult(
            routeOrder.getId(),
            routeOrder.getOrderNumber(),
            routeOrder.getStatus().name(),
            routeOrder.getCreatedAt()
        );
    }

}
