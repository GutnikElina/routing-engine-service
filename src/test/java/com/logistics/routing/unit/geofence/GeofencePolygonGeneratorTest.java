package com.logistics.routing.unit.geofence;

import com.logistics.routing.domain.geofence.service.GeofencePolygonGenerator;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeofencePolygonGeneratorTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final double METERS_PER_DEGREE_LATITUDE = 111_320.0;

    @Test
    void generatesAValidClosedPolygonCenteredOnThePoint() {
        BigDecimal latitude = new BigDecimal("53.9006");
        BigDecimal longitude = new BigDecimal("27.5590");
        BigDecimal radiusMeters = new BigDecimal("150");

        Polygon polygon = GeofencePolygonGenerator.generateCircularZone(latitude, longitude, radiusMeters);

        assertThat(polygon.isValid()).isTrue();
        assertThat(polygon.getSRID()).isEqualTo(4326);

        Point center = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
        assertThat(polygon.contains(center)).isTrue();
    }

    @Test
    void approximatesTheRequestedRadiusInMeters() {
        BigDecimal latitude = new BigDecimal("53.9006");
        BigDecimal longitude = new BigDecimal("27.5590");
        double radiusMeters = 200.0;

        Polygon polygon = GeofencePolygonGenerator.generateCircularZone(
                latitude, longitude, BigDecimal.valueOf(radiusMeters)
        );

        double metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(latitude.doubleValue()));
        double maxDistanceMeters = 0;
        for (Coordinate coordinate : polygon.getCoordinates()) {
            double dx = (coordinate.x - longitude.doubleValue()) * metersPerDegreeLongitude;
            double dy = (coordinate.y - latitude.doubleValue()) * METERS_PER_DEGREE_LATITUDE;
            maxDistanceMeters = Math.max(maxDistanceMeters, Math.sqrt(dx * dx + dy * dy));
        }

        assertThat(maxDistanceMeters).isCloseTo(radiusMeters, org.assertj.core.data.Percentage.withPercentage(2));
    }

    @Test
    void throwsWhenRadiusIsNotPositive() {
        assertThatThrownBy(() -> GeofencePolygonGenerator.generateCircularZone(
                new BigDecimal("53.9006"), new BigDecimal("27.5590"), BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsWhenCoordinatesAreNull() {
        assertThatThrownBy(() -> GeofencePolygonGenerator.generateCircularZone(
                null, new BigDecimal("27.5590"), new BigDecimal("150")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
