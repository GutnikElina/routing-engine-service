package com.logistics.routing.domain.route.service;

import com.logistics.routing.domain.route.exception.RouteSegmentationException;
import com.logistics.routing.domain.route.model.RouteSegment;
import com.logistics.routing.domain.route.model.Waypoint;
import com.logistics.routing.domain.route.model.enums.TransportType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Splits an ordered chain of waypoints into contiguous {@link RouteSegment}s, one per
 * run of consecutive legs sharing the same {@link TransportType}. The waypoint where the
 * mode changes (a Cross-dock/Customs hub) is shared between the closing and opening segment,
 * matching the M..N route_segment_waypoints junction table.
 */
public final class RouteSegmenter {

    private RouteSegmenter() {
    }

    /**
     * @param waypoints        the route's waypoints, at least 2, in any order (sorted by sequence number here)
     * @param legTransportTypes the transport mode used for each leg between consecutive waypoints;
     *                          must have exactly {@code waypoints.size() - 1} entries
     */
    public static List<RouteSegment> segment(List<Waypoint> waypoints, List<TransportType> legTransportTypes) {
        List<Waypoint> sortedWaypoints = validateAndSort(waypoints, legTransportTypes);

        List<RouteSegment> segments = new ArrayList<>();
        int segmentIndex = 0;
        int groupStart = 0;
        int legCount = legTransportTypes.size();

        for (int leg = 0; leg < legCount; leg++) {
            boolean isLastLeg = leg == legCount - 1;
            boolean modeChangesAfterThisLeg = !isLastLeg
                    && legTransportTypes.get(leg) != legTransportTypes.get(leg + 1);

            if (isLastLeg || modeChangesAfterThisLeg) {
                List<Waypoint> segmentWaypoints = sortedWaypoints.subList(groupStart, leg + 2);
                segments.add(RouteSegment.create(
                        null,
                        segmentIndex++,
                        legTransportTypes.get(leg),
                        segmentWaypoints,
                        null,
                        null,
                        null,
                        null
                ));
                groupStart = leg + 1;
            }
        }

        return segments;
    }

    private static List<Waypoint> validateAndSort(List<Waypoint> waypoints, List<TransportType> legTransportTypes) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new RouteSegmentationException("segmentation requires at least two waypoints");
        }
        if (legTransportTypes == null || legTransportTypes.size() != waypoints.size() - 1) {
            throw new RouteSegmentationException(
                    "legTransportTypes must have exactly one entry per leg (waypoints.size() - 1)"
            );
        }
        for (TransportType legTransportType : legTransportTypes) {
            if (legTransportType == null) {
                throw new RouteSegmentationException("legTransportTypes must not contain null entries");
            }
        }

        return waypoints.stream()
                .sorted(Comparator.comparingInt(Waypoint::getSequenceNumber))
                .toList();
    }
}
