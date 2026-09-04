package com.logistics.routing.application.geofence.generate;

import com.logistics.routing.application.port.out.GeofenceZonePersistencePort;
import com.logistics.routing.application.port.out.OutboxEventPort;
import com.logistics.routing.config.GeofenceProperties;
import com.logistics.routing.domain.geofence.model.GeofenceZone;
import com.logistics.routing.domain.geofence.service.GeofencePolygonGenerator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GenerateGeofenceZoneService implements GenerateGeofenceZoneUseCase {

    private final GeofenceZonePersistencePort geofenceZonePersistencePort;
    private final OutboxEventPort outboxEventPort;
    private final GeofenceProperties geofenceProperties;
    private final Clock clock;

    @Override
    @Transactional
    public GenerateGeofenceZoneResult execute(GenerateGeofenceZoneCommand command) {
        Instant now = Instant.now(clock);
        BigDecimal radiusMeters = command.radiusMeters() != null
                ? command.radiusMeters()
                : geofenceProperties.getDefaultRadiusMeters();

        Polygon polygon = GeofencePolygonGenerator.generateCircularZone(
                command.latitude(), command.longitude(), radiusMeters
        );

        GeofenceZone zone = GeofenceZone.create(
                null,
                command.name(),
                command.type(),
                command.sourceWaypointId(),
                polygon,
                now
        );

        UUID zoneId = geofenceZonePersistencePort.save(zone);
        outboxEventPort.append(new GeofenceZoneCreatedEvent(zoneId, command.name(), command.type(), now));

        return new GenerateGeofenceZoneResult(zoneId, command.name(), command.type(), now);
    }
}
