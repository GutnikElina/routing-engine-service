package com.logistics.routing.adapter.out.straightline;

import com.logistics.routing.application.routing.GeoCoordinate;
import com.logistics.routing.domain.route.exception.RoutingEngineException;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@UtilityClass
public class StraightLineCalculator {

    private static final MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
    private static final BigDecimal EARTH_RADIUS_METERS = new BigDecimal("6371000");

    public BigDecimal distanceMeters(GeoCoordinate from, GeoCoordinate to) {
        double centralAngle = centralAngleRadians(
                toRadians(from.latitude()),
                toRadians(from.longitude()),
                toRadians(to.latitude()),
                toRadians(to.longitude())
        );
        return BigDecimal.valueOf(centralAngle).multiply(EARTH_RADIUS_METERS, MATH_CONTEXT);
    }

    public BigDecimal durationSeconds(BigDecimal distanceMeters, BigDecimal averageSpeedKmh) {
        BigDecimal speedMetersPerSecond = averageSpeedKmh
                .multiply(new BigDecimal("1000"), MATH_CONTEXT)
                .divide(new BigDecimal("3600"), MATH_CONTEXT);

        if (speedMetersPerSecond.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RoutingEngineException("Average speed must be greater than zero");
        }

        return distanceMeters.divide(speedMetersPerSecond, MATH_CONTEXT);
    }

    public GeoCoordinate interpolate(GeoCoordinate from, GeoCoordinate to, BigDecimal fraction) {
        double lat1 = toRadians(from.latitude());
        double lon1 = toRadians(from.longitude());
        double lat2 = toRadians(to.latitude());
        double lon2 = toRadians(to.longitude());
        double fractionValue = fraction.doubleValue();

        if (fractionValue <= 0.0) {
            return from;
        }
        if (fractionValue >= 1.0) {
            return to;
        }

        double delta = centralAngleRadians(lat1, lon1, lat2, lon2);
        if (delta == 0.0) {
            return from;
        }

        double sinDelta = Math.sin(delta);
        double a = Math.sin((1.0 - fractionValue) * delta) / sinDelta;
        double b = Math.sin(fractionValue * delta) / sinDelta;

        double x = a * Math.cos(lat1) * Math.cos(lon1) + b * Math.cos(lat2) * Math.cos(lon2);
        double y = a * Math.cos(lat1) * Math.sin(lon1) + b * Math.cos(lat2) * Math.sin(lon2);
        double z = a * Math.sin(lat1) + b * Math.sin(lat2);

        double latitude = Math.atan2(z, Math.sqrt(x * x + y * y));
        double longitude = Math.atan2(y, x);

        return new GeoCoordinate(toDegreesBigDecimal(latitude), toDegreesBigDecimal(longitude));
    }

    private double centralAngleRadians(double lat1, double lon1, double lat2, double lon2) {
        double haversine = Math.pow(Math.sin((lat2 - lat1) / 2.0), 2.0)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin((lon2 - lon1) / 2.0), 2.0);
        return 2.0 * Math.asin(Math.min(1.0, Math.sqrt(haversine)));
    }

    private double toRadians(BigDecimal degrees) {
        return Math.toRadians(degrees.doubleValue());
    }

    private BigDecimal toDegreesBigDecimal(double radians) {
        return BigDecimal.valueOf(Math.toDegrees(radians));
    }
}
