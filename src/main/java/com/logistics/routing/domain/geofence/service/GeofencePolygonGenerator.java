package com.logistics.routing.domain.geofence.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Generates a circular geofence polygon (in WGS 84 / SRID 4326 degrees) around a center point.
 * Buffering happens in a local equirectangular approximation (meters, scaled by the center's
 * latitude) so the result is an actual circle in meters rather than an ellipse distorted by
 * degree/meter ratio at that latitude; this is accurate enough for warehouse/port-sized zones
 * (tens to a few hundred meters) and not intended for continental-scale buffers.
 */
public final class GeofencePolygonGenerator {

    private static final int WGS84_SRID = 4326;
    private static final int BUFFER_QUADRANT_SEGMENTS = 16;
    private static final double METERS_PER_DEGREE_LATITUDE = 111_320.0;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private GeofencePolygonGenerator() {
    }

    public static Polygon generateCircularZone(BigDecimal latitude, BigDecimal longitude, BigDecimal radiusMeters) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("latitude and longitude must not be null");
        }
        if (radiusMeters == null || radiusMeters.signum() <= 0) {
            throw new IllegalArgumentException("radiusMeters must be positive");
        }

        double latDegrees = latitude.doubleValue();
        double lonDegrees = longitude.doubleValue();
        double metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(latDegrees));

        Point centerInMeters = GEOMETRY_FACTORY.createPoint(new Coordinate(
                lonDegrees * metersPerDegreeLongitude,
                latDegrees * METERS_PER_DEGREE_LATITUDE
        ));
        Polygon bufferedInMeters = (Polygon) centerInMeters.buffer(radiusMeters.doubleValue(), BUFFER_QUADRANT_SEGMENTS);

        Coordinate[] degreeCoordinates = Arrays.stream(bufferedInMeters.getCoordinates())
                .map(c -> new Coordinate(c.x / metersPerDegreeLongitude, c.y / METERS_PER_DEGREE_LATITUDE))
                .toArray(Coordinate[]::new);

        Polygon polygon = GEOMETRY_FACTORY.createPolygon(degreeCoordinates);
        polygon.setSRID(WGS84_SRID);
        return polygon;
    }
}
