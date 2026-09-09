package com.logistics.routing.unit.osrm;

import com.logistics.routing.adapter.out.osrm.OsrmRoutingClient;
import com.logistics.routing.adapter.out.osrm.dto.OsrmRouteResponse;
import com.logistics.routing.adapter.out.osrm.dto.OsrmTableResponse;
import com.logistics.routing.application.routing.DistanceMatrix;
import com.logistics.routing.application.routing.RouteGeometry;
import com.logistics.routing.domain.route.exception.RoutingEngineException;
import com.logistics.routing.domain.route.exception.RoutingEngineUnavailableException;
import com.logistics.routing.domain.route.model.enums.TransportType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;

import static com.logistics.routing.testdata.OsrmTestData.coordinate;
import static com.logistics.routing.testdata.OsrmTestData.okRouteResponse;
import static com.logistics.routing.testdata.OsrmTestData.okTableResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OsrmRoutingClientTest {

    private static final String PROFILE = "driving";
    private static final String ROUTE_OVERVIEW = "simplified";

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OsrmRoutingClient client;

    @BeforeEach
    void setUp() {
        client = new OsrmRoutingClient(
                restClient,
                PROFILE,
                ROUTE_OVERVIEW,
                OsrmRoutingClient.resilienceInstanceName(TransportType.TRUCK),
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build())
        );
    }

    private void stubTableRequestChain() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/table/v1/{profile}"), eq(PROFILE))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
    }

    private void stubRouteRequestChain() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/route/v1/{profile}"), eq(PROFILE))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
    }

    private void mockTableSuccess(OsrmTableResponse response) {
        stubTableRequestChain();
        when(responseSpec.body(OsrmTableResponse.class)).thenReturn(response);
    }

    private void mockRouteSuccess(OsrmRouteResponse response) {
        stubRouteRequestChain();
        when(responseSpec.body(OsrmRouteResponse.class)).thenReturn(response);
    }

    private void mockTableThenThrow(RuntimeException exception) {
        stubTableRequestChain();
        doThrow(exception).when(responseSpec).body(OsrmTableResponse.class);
    }

    private void mockRouteThenThrow(RuntimeException exception) {
        stubRouteRequestChain();
        doThrow(exception).when(responseSpec).body(OsrmRouteResponse.class);
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
        void shouldThrowRoutingEngineExceptionWhenTableContainsNullCell() {
            mockTableSuccess(okTableResponse(
                    List.of(Arrays.asList(0.0, null), List.of(5000.0, 0.0)),
                    List.of(List.of(0.0, 5000.0), List.of(5000.0, 0.0))));

            assertThatThrownBy(() -> client.getDistanceMatrix(
                    List.of(coordinate(55, 37), coordinate(56, 38))))
                    .isInstanceOf(RoutingEngineException.class)
                    .hasMessage("OSRM table response contains null values in durations");
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
