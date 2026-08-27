package com.logistics.routing.adapter.out.persistence.route;

import com.logistics.routing.domain.route.model.RouteOrder;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RouteOrderPersistenceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "waypoints", ignore = true)
    RouteOrderEntity toEntity(RouteOrder routeOrder);

}
