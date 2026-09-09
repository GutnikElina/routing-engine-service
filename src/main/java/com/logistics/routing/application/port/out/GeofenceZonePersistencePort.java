package com.logistics.routing.application.port.out;

import com.logistics.routing.domain.geofence.model.GeofenceZone;

import java.util.UUID;

public interface GeofenceZonePersistencePort {
    UUID save(GeofenceZone zone);
}
