package com.logistics.routing.adapter.in.web.mapper;

import org.mapstruct.Mapper;

import com.logistics.routing.adapter.in.web.dto.request.CreateRouteRequest;
import com.logistics.routing.adapter.in.web.dto.response.CreateRouteResponse;
import com.logistics.routing.application.route.create.CreateRouteCommand;
import com.logistics.routing.application.route.create.CreateRouteResult;
import com.logistics.routing.application.route.create.CargoCommand;
import com.logistics.routing.application.route.create.WaypointCommand;
import com.logistics.routing.adapter.in.web.dto.request.CargoRequest;
import com.logistics.routing.adapter.in.web.dto.request.WaypointRequest;

@Mapper(componentModel = "spring")
public interface RouteWebMapper {
    CreateRouteCommand toCommand(CreateRouteRequest request);
    CreateRouteResponse toResponse(CreateRouteResult result);

    CargoCommand toCommand(CargoRequest request);
    WaypointCommand toCommand(WaypointRequest request);
}
