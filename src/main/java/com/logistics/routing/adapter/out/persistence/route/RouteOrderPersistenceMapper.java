package com.logistics.routing.adapter.out.persistence.route;

import com.logistics.routing.domain.route.model.RouteOrder;
import com.logistics.routing.domain.route.model.enums.RouteOrderStatus;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RouteOrderPersistenceMapper {

    RouteOrderEntity toEntity(RouteOrder routeOrder);

    default RouteOrderStatus toPersistenceStatus(
            RouteOrderStatus status
    ) {
        if (status == RouteOrderStatus.DRAFT) {
            return RouteOrderStatus.DRAFT;
        }

        throw new IllegalArgumentException(
                "Persistence status mapping is not defined for: " + status
        );
    }
}
