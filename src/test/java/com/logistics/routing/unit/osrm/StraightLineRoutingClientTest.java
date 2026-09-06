package com.logistics.routing.unit.osrm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import com.logistics.routing.adapter.out.straightline.StraightLineRoutingClient;
import com.logistics.routing.application.routing.GeoCoordinate;
import com.logistics.routing.application.routing.RouteGeometry;
import com.logistics.routing.domain.route.exception.RoutingEngineException;
import com.logistics.routing.application.routing.DistanceMatrix;
import com.logistics.routing.adapter.out.straightline.StraightLineCalculator;

import static com.logistics.routing.testdata.OsrmTestData.coordinate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.util.List;

class StraightLineRoutingClientTest {

    private static final BigDecimal AVERAGE_SPEED = BigDecimal.valueOf(100);

    private final StraightLineRoutingClient client = new StraightLineRoutingClient(AVERAGE_SPEED);

    @Test
    void shouldRejectNullAverageSpeed() {
        assertThatThrownBy(() -> new StraightLineRoutingClient(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Average speed must be greater than zero");
    }

    @Test
    void shouldRejectZeroAverageSpeed() {
        assertThatThrownBy(() -> new StraightLineRoutingClient(BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Average speed must be greater than zero");
    }

    @Test
    void shouldRejectNegativeAverageSpeed() {
        assertThatThrownBy(() -> new StraightLineRoutingClient(BigDecimal.valueOf(-10)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Average speed must be greater than zero");
    }


    @Nested
    class GetDistanceMatrixTest {

        @Test
        void shouldReturnZeroDistanceAndDurationForSameCoordinate() {
            GeoCoordinate coordinate = coordinate(55, 37);
    
            DistanceMatrix result = client.getDistanceMatrix(
                    List.of(coordinate)
            );
    
            assertThat(result.durationsSeconds())
                    .containsExactly(List.of(BigDecimal.ZERO));
    
            assertThat(result.distancesMeters())
                    .containsExactly(List.of(BigDecimal.ZERO));
        }
    
        @Test
        void shouldCalculateSymmetricDistanceMatrix() {
            GeoCoordinate from = coordinate(55, 37);
            GeoCoordinate to = coordinate(56, 38);
    
            DistanceMatrix result = client.getDistanceMatrix(
                    List.of(from, to)
            );
    
            BigDecimal fromTo = result.distancesMeters().get(0).get(1);
            BigDecimal toFrom = result.distancesMeters().get(1).get(0);
    
            assertThat(fromTo)
                    .isPositive()
                    .isEqualByComparingTo(toFrom);
        }
    
        @Test
        void shouldCalculateSymmetricDurationMatrix() {
            GeoCoordinate from = coordinate(55, 37);
            GeoCoordinate to = coordinate(56, 38);
    
            DistanceMatrix result = client.getDistanceMatrix(
                    List.of(from, to)
            );
    
            BigDecimal fromTo = result.durationsSeconds().get(0).get(1);
            BigDecimal toFrom = result.durationsSeconds().get(1).get(0);
    
            assertThat(fromTo)
                    .isPositive()
                    .isEqualByComparingTo(toFrom);
        }
    
        @Test
        void shouldReturnDistanceMatrixForMultipleCoordinates() {
            List<GeoCoordinate> coordinates = List.of(
                    coordinate(55, 37),
                    coordinate(56, 38),
                    coordinate(57, 39)
            );
    
            DistanceMatrix result = client.getDistanceMatrix(coordinates);
    
            assertThat(result.distancesMeters())
                    .hasSize(3)
                    .allSatisfy(row -> assertThat(row).hasSize(3));
    
            assertThat(result.durationsSeconds())
                    .hasSize(3)
                    .allSatisfy(row -> assertThat(row).hasSize(3));
        }
    
        @Test
        void shouldReturnZeroOnDistanceMatrixDiagonal() {
            List<GeoCoordinate> coordinates = List.of(
                    coordinate(55, 37),
                    coordinate(56, 38),
                    coordinate(57, 39)
            );
    
            DistanceMatrix result = client.getDistanceMatrix(coordinates);
    
            assertThat(result.distancesMeters().get(0).get(0))
                    .isEqualByComparingTo(BigDecimal.ZERO);
    
            assertThat(result.distancesMeters().get(1).get(1))
                    .isEqualByComparingTo(BigDecimal.ZERO);
    
            assertThat(result.distancesMeters().get(2).get(2))
                    .isEqualByComparingTo(BigDecimal.ZERO);
    
            assertThat(result.durationsSeconds().get(0).get(0))
                    .isEqualByComparingTo(BigDecimal.ZERO);
    
            assertThat(result.durationsSeconds().get(1).get(1))
                    .isEqualByComparingTo(BigDecimal.ZERO);
    
            assertThat(result.durationsSeconds().get(2).get(2))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

    }

    @Nested
    class GetRouteGeometryTest {
        @Test
        void shouldRejectRouteWithLessThanTwoWaypoints() {
            assertThatThrownBy(() ->
                    client.getRouteGeometry(
                            List.of(coordinate(55, 37))
                    )
            )
                    .isInstanceOf(RoutingEngineException.class)
                    .hasMessage("Route requires at least 2 waypoints");
        }
    
        @Test
        void shouldRejectEmptyRoute() {
            assertThatThrownBy(() ->
                    client.getRouteGeometry(List.of())
            )
                    .isInstanceOf(RoutingEngineException.class)
                    .hasMessage("Route requires at least 2 waypoints");
        }
    
        @Test
        void shouldBuildRouteBetweenTwoWaypoints() {
            GeoCoordinate from = coordinate(55, 37);
            GeoCoordinate to = coordinate(56, 38);
    
            RouteGeometry result = client.getRouteGeometry(
                    List.of(from, to)
            );
    
            assertThat(result.path())
                    .hasSize(17);
    
            assertThat(result.path().getFirst())
                    .isEqualTo(from);
    
            assertThat(result.path().getLast())
                    .isEqualTo(to);
    
            assertThat(result.distanceMeters())
                    .isPositive();
    
            assertThat(result.durationSeconds())
                    .isPositive();
        }
    
        @Test
        void shouldBuildPathForMultipleLegs() {
            GeoCoordinate first = coordinate(55, 37);
            GeoCoordinate second = coordinate(56, 38);
            GeoCoordinate third = coordinate(57, 39);
    
            RouteGeometry result = client.getRouteGeometry(
                    List.of(first, second, third)
            );
    
            assertThat(result.path())
                    .hasSize(33);
    
            assertThat(result.path().getFirst())
                    .isEqualTo(first);
    
            assertThat(result.path().get(16))
                    .isEqualTo(second);
    
            assertThat(result.path().getLast())
                    .isEqualTo(third);
        }
    
        @Test
        void shouldCalculateTotalDistanceForMultipleLegs() {
            GeoCoordinate first = coordinate(55, 37);
            GeoCoordinate second = coordinate(56, 38);
            GeoCoordinate third = coordinate(57, 39);
    
            RouteGeometry result = client.getRouteGeometry(
                    List.of(first, second, third)
            );
    
            BigDecimal firstLeg = StraightLineCalculator.distanceMeters(
                    first,
                    second
            );
    
            BigDecimal secondLeg = StraightLineCalculator.distanceMeters(
                    second,
                    third
            );
    
            BigDecimal expected = firstLeg.add(secondLeg);
    
            assertThat(result.distanceMeters())
                    .isEqualByComparingTo(expected);
        }
    
        @Test
        void shouldCalculateTotalDurationForMultipleLegs() {
            GeoCoordinate first = coordinate(55, 37);
            GeoCoordinate second = coordinate(56, 38);
            GeoCoordinate third = coordinate(57, 39);
    
            RouteGeometry result = client.getRouteGeometry(
                    List.of(first, second, third)
            );
    
            BigDecimal firstLegDistance =
                    StraightLineCalculator.distanceMeters(first, second);
    
            BigDecimal secondLegDistance =
                    StraightLineCalculator.distanceMeters(second, third);
    
            BigDecimal expected =
                    StraightLineCalculator.durationSeconds(
                            firstLegDistance,
                            AVERAGE_SPEED
                    ).add(
                            StraightLineCalculator.durationSeconds(
                                    secondLegDistance,
                                    AVERAGE_SPEED
                            )
                    );
    
            assertThat(result.durationSeconds())
                    .isEqualByComparingTo(expected);
        }
    }
}
