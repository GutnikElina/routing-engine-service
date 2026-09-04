package com.logistics.routing.unit.geofence;

import com.logistics.routing.application.geofence.generate.GenerateGeofenceZoneCommand;
import com.logistics.routing.application.geofence.generate.GenerateGeofenceZoneResult;
import com.logistics.routing.application.geofence.generate.GenerateGeofenceZoneService;
import com.logistics.routing.application.geofence.generate.GeofenceZoneCreatedEvent;
import com.logistics.routing.application.port.out.GeofenceZonePersistencePort;
import com.logistics.routing.application.port.out.OutboxEventPort;
import com.logistics.routing.config.GeofenceProperties;
import com.logistics.routing.domain.geofence.model.GeofenceZone;
import com.logistics.routing.domain.geofence.model.GeofenceZoneType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateGeofenceZoneServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T08:00:00Z");
    private static final UUID ZONE_ID = UUID.fromString("0d2ce760-d04d-4eb6-b768-4924845eb650");

    @Mock
    private GeofenceZonePersistencePort geofenceZonePersistencePort;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Captor
    private ArgumentCaptor<GeofenceZone> geofenceZoneCaptor;

    @Captor
    private ArgumentCaptor<GeofenceZoneCreatedEvent> eventCaptor;

    private GenerateGeofenceZoneService service;

    @BeforeEach
    void setUp() {
        GeofenceProperties properties = new GeofenceProperties();
        properties.setDefaultRadiusMeters(new BigDecimal("150"));

        service = new GenerateGeofenceZoneService(
                geofenceZonePersistencePort,
                outboxEventPort,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void generatesPersistsAndPublishesAGeofenceZoneUsingTheRequestedRadius() {
        when(geofenceZonePersistencePort.save(any(GeofenceZone.class))).thenReturn(ZONE_ID);
        UUID sourceWaypointId = UUID.randomUUID();

        GenerateGeofenceZoneCommand command = new GenerateGeofenceZoneCommand(
                "Minsk Cross-dock",
                GeofenceZoneType.WAREHOUSE,
                new BigDecimal("53.9006"),
                new BigDecimal("27.5590"),
                new BigDecimal("300"),
                sourceWaypointId
        );

        GenerateGeofenceZoneResult result = service.execute(command);

        verify(geofenceZonePersistencePort).save(geofenceZoneCaptor.capture());
        verify(outboxEventPort).append(eventCaptor.capture());

        GeofenceZone savedZone = geofenceZoneCaptor.getValue();
        assertThat(savedZone.getName()).isEqualTo("Minsk Cross-dock");
        assertThat(savedZone.getType()).isEqualTo(GeofenceZoneType.WAREHOUSE);
        assertThat(savedZone.getSourceWaypointId()).isEqualTo(sourceWaypointId);
        assertThat(savedZone.getPolygon().isValid()).isTrue();

        GeofenceZoneCreatedEvent event = eventCaptor.getValue();
        assertThat(event.zoneId()).isEqualTo(ZONE_ID);
        assertThat(event.name()).isEqualTo("Minsk Cross-dock");
        assertThat(event.type()).isEqualTo(GeofenceZoneType.WAREHOUSE);
        assertThat(event.occurredAt()).isEqualTo(NOW);

        assertThat(result).extracting(
                GenerateGeofenceZoneResult::zoneId,
                GenerateGeofenceZoneResult::name,
                GenerateGeofenceZoneResult::type,
                GenerateGeofenceZoneResult::createdAt
        ).containsExactly(ZONE_ID, "Minsk Cross-dock", GeofenceZoneType.WAREHOUSE, NOW);
    }

    @Test
    void fallsBackToTheConfiguredDefaultRadiusWhenCommandOmitsIt() {
        when(geofenceZonePersistencePort.save(any(GeofenceZone.class))).thenReturn(ZONE_ID);

        GenerateGeofenceZoneCommand command = new GenerateGeofenceZoneCommand(
                "Gdansk Port",
                GeofenceZoneType.PORT,
                new BigDecimal("54.3520"),
                new BigDecimal("18.6466"),
                null,
                null
        );

        service.execute(command);

        verify(geofenceZonePersistencePort).save(geofenceZoneCaptor.capture());
        assertThat(geofenceZoneCaptor.getValue().getPolygon().isValid()).isTrue();
        assertThat(geofenceZoneCaptor.getValue().getSourceWaypointId()).isNull();
    }
}
