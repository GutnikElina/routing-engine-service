package com.logistics.routing.unit.osrm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.logistics.routing.adapter.out.osrm.OsrmRoutingClient;
import com.logistics.routing.adapter.out.osrm.dto.OsrmRouteResponse;
import com.logistics.routing.adapter.out.osrm.dto.OsrmTableResponse;
import com.logistics.routing.application.routing.DistanceMatrix;
import com.logistics.routing.application.routing.RouteGeometry;
import com.logistics.routing.domain.route.exception.RoutingEngineException;
import com.logistics.routing.domain.route.exception.RoutingEngineUnavailableException;

import static com.logistics.routing.testdata.OsrmTestData.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import org.mockito.Mockito;

import java.net.SocketTimeoutException;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OsrmRoutingClientTest {

    private static final String PROFILE = "driving";
    private static final String ROUTE_OVERVIEW = "simplified";

    private final RestClient restClient = Mockito.mock(RestClient.class, RETURNS_DEEP_STUBS);

    private OsrmRoutingClient client;

    @BeforeEach
    void setUp() {
        client = new OsrmRoutingClient(restClient, PROFILE, ROUTE_OVERVIEW);
    }

    private void mockTableSuccess(OsrmTableResponse response) {
        when(restClient.post().uri(anyString(), anyString()).contentType(any()).body(any()).retrieve()
                .onStatus(any(), any()).body(OsrmTableResponse.class)).thenReturn(response);
    }

    private void mockRouteSuccess(OsrmRouteResponse response) {
        when(restClient.post().uri(anyString(), anyString()).contentType(any()).body(any()).retrieve()
                .onStatus(any(), any()).body(OsrmRouteResponse.class)).thenReturn(response);
    }

    private void mockTableThenThrow(RuntimeException ex) {
        when(restClient.post().uri(anyString(), anyString()).contentType(any()).body(any()).retrieve()
                .onStatus(any(), any()).body(OsrmTableResponse.class)).thenThrow(ex);
    }

    private void mockRouteThenThrow(RuntimeException ex) {
        when(restClient.post().uri(anyString(), anyString()).contentType(any()).body(any()).retrieve()
                .onStatus(any(), any()).body(OsrmRouteResponse.class)).thenThrow(ex);
    }

    @Nested
    class GetDistanceMatrix {

        @Test
        void shouldReturnDistanceMatrix() {
            mockTableSuccess(okTableResponse(
                    List.of(List.of(0.0, 5000.0), List.of(5000.0, 0.0)),
                    List.of(List.of(0.0, 5000.0), List.of(5000.0, 0.0))));

            DistanceMatrix result = client.getDistanceMatrix(
                    List.of(coordinate(55, 37), coordinate(56, 38)));

            assertThat(result.distancesMeters().get(0).get(1)).isPositive();
            assertThat(result.durationsSeconds().get(0).get(1)).isPositive();
        }

        @Test
        void shouldThrowUnavailableOnConnectionFailure() {
            mockTableThenThrow(new ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> client.getDistanceMatrix(List.of(coordinate(55, 37))))
                    .isInstanceOf(RoutingEngineUnavailableException.class)
                    .hasMessage("OSRM service unavailable");
        }

        @Test
        void shouldThrowUnavailableOnTimeout() {
            mockTableThenThrow(new ResourceAccessException("Read timeout", new SocketTimeoutException()));

            assertThatThrownBy(() -> client.getDistanceMatrix(List.of(coordinate(55, 37))))
                    .isInstanceOf(RoutingEngineUnavailableException.class)
                    .hasMessage("OSRM service unavailable");
        }

        @Test
        void shouldThrowRoutingEngineExceptionOnUnexpectedClientFailure() {
            mockTableThenThrow(new RestClientException("Unexpected error"));

            assertThatThrownBy(() -> client.getDistanceMatrix(List.of(coordinate(55, 37))))
                    .isInstanceOf(RoutingEngineException.class)
                    .hasMessage("OSRM table request failed");
        }
    }

    @Nested
    class GetRouteGeometry {

        @Test
        void shouldReturnRouteGeometry() {
            mockRouteSuccess(okRouteResponse(
                    List.of(List.of(37.0, 55.0), List.of(38.0, 56.0)), 5000.0, 50.0));

            RouteGeometry result = client.getRouteGeometry(
                    List.of(coordinate(55, 37), coordinate(56, 38)));

            assertThat(result.path()).hasSize(2);
            assertThat(result.distanceMeters()).isPositive();
            assertThat(result.durationSeconds()).isPositive();
        }

        @Test
        void shouldThrowUnavailableOnConnectionFailure() {
            mockRouteThenThrow(new ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> client.getRouteGeometry(List.of(coordinate(55, 37), coordinate(56, 38))))
                    .isInstanceOf(RoutingEngineUnavailableException.class)
                    .hasMessage("OSRM service unavailable");
        }

        @Test
        void shouldThrowUnavailableOnTimeout() {
            mockRouteThenThrow(new ResourceAccessException("Read timeout", new SocketTimeoutException()));

            assertThatThrownBy(() -> client.getRouteGeometry(List.of(coordinate(55, 37), coordinate(56, 38))))
                    .isInstanceOf(RoutingEngineUnavailableException.class)
                    .hasMessage("OSRM service unavailable");
        }

        @Test
        void shouldThrowRoutingEngineExceptionOnUnexpectedClientFailure() {
            mockRouteThenThrow(new RestClientException("Unexpected error"));

            assertThatThrownBy(() -> client.getRouteGeometry(List.of(coordinate(55, 37), coordinate(56, 38))))
                    .isInstanceOf(RoutingEngineException.class)
                    .hasMessage("OSRM route request failed");
        }
    }

    @Nested
    class HttpErrorHandling {

        @Test
        void shouldThrowRoutingEngineExceptionOn4xxResponse() {
            mockTableThenThrow(new RoutingEngineException("OSRM request failed with HTTP 400"));

            assertThatThrownBy(() -> client.getDistanceMatrix(List.of(coordinate(55, 37))))
                    .isInstanceOf(RoutingEngineException.class)
                    .hasMessage("OSRM request failed with HTTP 400");
        }

        @Test
        void shouldThrowRoutingEngineExceptionOn5xxResponse() {
            mockTableThenThrow(new RoutingEngineException("OSRM request failed with HTTP 500"));

            assertThatThrownBy(() -> client.getDistanceMatrix(List.of(coordinate(55, 37))))
                    .isInstanceOf(RoutingEngineException.class)
                    .hasMessage("OSRM request failed with HTTP 500");
        }
    }
}
