package com.logistics.routing.unit.route;

import com.logistics.routing.domain.route.exception.RouteSegmentationException;
import com.logistics.routing.domain.route.model.RouteSegment;
import com.logistics.routing.domain.route.model.Waypoint;
import com.logistics.routing.domain.route.model.enums.TransportType;
import com.logistics.routing.domain.route.model.enums.WaypointType;
import com.logistics.routing.domain.route.service.RouteSegmenter;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteSegmenterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T08:00:00Z");

    @Test
    void groupsConsecutiveLegsWithTheSameTransportTypeIntoOneSegment() {
        Waypoint origin = waypoint(WaypointType.ORIGIN, 1);
        Waypoint hub = waypoint(WaypointType.CROSS_DOCK, 2);
        Waypoint destination = waypoint(WaypointType.DESTINATION, 3);

        List<RouteSegment> segments = RouteSegmenter.segment(
                List.of(origin, hub, destination),
                List.of(TransportType.TRUCK, TransportType.TRUCK)
        );

        assertThat(segments).hasSize(1);
        RouteSegment segment = segments.getFirst();
        assertThat(segment.getSegmentIndex()).isZero();
        assertThat(segment.getTransportType()).isEqualTo(TransportType.TRUCK);
        assertThat(segment.getWaypoints()).containsExactly(origin, hub, destination);
    }

    @Test
    void splitsIntoMultipleSegmentsOnTransportModeChangeSharingTheHubWaypoint() {
        Waypoint origin = waypoint(WaypointType.ORIGIN, 1);
        Waypoint port = waypoint(WaypointType.CROSS_DOCK, 2);
        Waypoint destination = waypoint(WaypointType.DESTINATION, 3);

        List<RouteSegment> segments = RouteSegmenter.segment(
                List.of(origin, port, destination),
                List.of(TransportType.TRUCK, TransportType.VESSEL)
        );

        assertThat(segments).hasSize(2);

        RouteSegment truckSegment = segments.get(0);
        assertThat(truckSegment.getSegmentIndex()).isZero();
        assertThat(truckSegment.getTransportType()).isEqualTo(TransportType.TRUCK);
        assertThat(truckSegment.getWaypoints()).containsExactly(origin, port);

        RouteSegment vesselSegment = segments.get(1);
        assertThat(vesselSegment.getSegmentIndex()).isEqualTo(1);
        assertThat(vesselSegment.getTransportType()).isEqualTo(TransportType.VESSEL);
        assertThat(vesselSegment.getWaypoints()).containsExactly(port, destination);
    }

    @Test
    void handlesMultimodalChainAcrossThreeModes() {
        Waypoint origin = waypoint(WaypointType.ORIGIN, 1);
        Waypoint port = waypoint(WaypointType.CROSS_DOCK, 2);
        Waypoint railHub = waypoint(WaypointType.CROSS_DOCK, 3);
        Waypoint destination = waypoint(WaypointType.DESTINATION, 4);

        List<RouteSegment> segments = RouteSegmenter.segment(
                List.of(origin, port, railHub, destination),
                List.of(TransportType.TRUCK, TransportType.VESSEL, TransportType.TRAIN)
        );

        assertThat(segments).extracting(RouteSegment::getTransportType)
                .containsExactly(TransportType.TRUCK, TransportType.VESSEL, TransportType.TRAIN);
        assertThat(segments).extracting(RouteSegment::getSegmentIndex)
                .containsExactly(0, 1, 2);
    }

    @Test
    void sortsWaypointsBySequenceNumberBeforeSegmenting() {
        Waypoint destination = waypoint(WaypointType.DESTINATION, 2);
        Waypoint origin = waypoint(WaypointType.ORIGIN, 1);

        List<RouteSegment> segments = RouteSegmenter.segment(
                List.of(destination, origin),
                List.of(TransportType.TRUCK)
        );

        assertThat(segments.getFirst().getWaypoints()).containsExactly(origin, destination);
    }

    @Test
    void throwsWhenFewerThanTwoWaypoints() {
        assertThatThrownBy(() -> RouteSegmenter.segment(List.of(waypoint(WaypointType.ORIGIN, 1)), List.of()))
                .isInstanceOf(RouteSegmentationException.class);
    }

    @Test
    void throwsWhenLegTransportTypesSizeDoesNotMatchWaypoints() {
        List<Waypoint> waypoints = List.of(
                waypoint(WaypointType.ORIGIN, 1),
                waypoint(WaypointType.DESTINATION, 2)
        );

        assertThatThrownBy(() -> RouteSegmenter.segment(waypoints, List.of(TransportType.TRUCK, TransportType.TRAIN)))
                .isInstanceOf(RouteSegmentationException.class);
    }

    @Test
    void throwsWhenALegTransportTypeIsNull() {
        List<Waypoint> waypoints = List.of(
                waypoint(WaypointType.ORIGIN, 1),
                waypoint(WaypointType.CROSS_DOCK, 2),
                waypoint(WaypointType.DESTINATION, 3)
        );
        List<TransportType> legTypes = new java.util.ArrayList<>(List.of(TransportType.TRUCK));
        legTypes.add(null);

        assertThatThrownBy(() -> RouteSegmenter.segment(waypoints, legTypes))
                .isInstanceOf(RouteSegmentationException.class);
    }

    private static Waypoint waypoint(WaypointType type, int sequence) {
        return new Waypoint(
                UUID.randomUUID(),
                type,
                sequence,
                new BigDecimal("50.000000"),
                new BigDecimal("20.000000"),
                "Test address " + sequence,
                null,
                null,
                NOW,
                NOW
        );
    }
}
