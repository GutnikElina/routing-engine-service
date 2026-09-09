package com.logistics.routing.adapter.out.persistence.route;

import com.logistics.routing.domain.route.model.Waypoint;
import lombok.experimental.UtilityClass;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

@UtilityClass
public class WaypointPersistenceMapper {

    private static final int WGS_84_SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public WaypointEntity toEntity(Waypoint waypoint) {
        return WaypointEntity.builder()
                .type(waypoint.getType())
                .sequenceNumber(waypoint.getSequenceNumber())
                .location(toPoint(waypoint))
                .address(waypoint.getAddress())
                .timeWindowStart(waypoint.getTimeWindowStart())
                .timeWindowEnd(waypoint.getTimeWindowEnd())
                .createdAt(waypoint.getCreatedAt())
                .updatedAt(waypoint.getUpdatedAt())
                .build();
    }

    private Point toPoint(Waypoint waypoint) {
        Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(waypoint.getLongitude().doubleValue(), waypoint.getLatitude().doubleValue())
        );
        point.setSRID(WGS_84_SRID);
        return point;
    }
}
