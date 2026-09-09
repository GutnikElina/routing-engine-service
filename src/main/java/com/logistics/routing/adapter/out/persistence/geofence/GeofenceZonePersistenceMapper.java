package com.logistics.routing.adapter.out.persistence.geofence;

import com.logistics.routing.domain.geofence.model.GeofenceZone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GeofenceZonePersistenceMapper {

    @Mapping(target = "id", ignore = true)
    GeofenceZoneEntity toEntity(GeofenceZone zone);
}
