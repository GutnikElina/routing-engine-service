package com.logistics.routing.adapter.out.persistence.geofence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GeofenceZoneRepository extends JpaRepository<GeofenceZoneEntity, UUID> {
}
