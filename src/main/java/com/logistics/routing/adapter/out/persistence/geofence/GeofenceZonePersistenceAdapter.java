package com.logistics.routing.adapter.out.persistence.geofence;

import com.logistics.routing.application.port.out.GeofenceZonePersistencePort;
import com.logistics.routing.domain.geofence.model.GeofenceZone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GeofenceZonePersistenceAdapter implements GeofenceZonePersistencePort {

    private final GeofenceZoneRepository geofenceZoneRepository;
    private final GeofenceZonePersistenceMapper geofenceZonePersistenceMapper;

    @Override
    public UUID save(GeofenceZone zone) {
        GeofenceZoneEntity entity = geofenceZonePersistenceMapper.toEntity(zone);
        return geofenceZoneRepository.save(entity).getId();
    }
}
