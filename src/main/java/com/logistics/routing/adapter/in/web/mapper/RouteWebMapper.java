package com.logistics.routing.adapter.in.web.mapper;

import org.mapstruct.Mapper;

import com.logistics.routing.adapter.in.web.generated.model.CargoRequest;
import com.logistics.routing.adapter.in.web.generated.model.CreateRouteRequest;
import com.logistics.routing.adapter.in.web.generated.model.CreateRouteResponse;
import com.logistics.routing.adapter.in.web.generated.model.WaypointRequest;
import com.logistics.routing.application.route.create.CreateRouteCommand;
import com.logistics.routing.application.route.create.CreateRouteResult;
import com.logistics.routing.application.route.create.CargoCommand;
import com.logistics.routing.application.route.create.WaypointCommand;
import com.logistics.routing.domain.route.model.enums.WaypointType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface RouteWebMapper {
    CreateRouteCommand toCommand(CreateRouteRequest request);
    CreateRouteResponse toResponse(CreateRouteResult result);

    CargoCommand toCommand(CargoRequest request);
    WaypointCommand toCommand(WaypointRequest request);

    default String map(CargoRequest.AdrClassEnum adrClass) {
        return adrClass == null ? null : adrClass.getValue();
    }

    default WaypointType map(WaypointRequest.TypeEnum waypointType) {
        return waypointType == null ? null : WaypointType.valueOf(waypointType.name());
    }

    default Instant map(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    default OffsetDateTime map(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
