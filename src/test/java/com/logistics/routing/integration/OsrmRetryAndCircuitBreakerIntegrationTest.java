package com.logistics.routing.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.logistics.routing.adapter.out.osrm.OsrmRoutingClient;
import com.logistics.routing.adapter.out.routing.RoutingClient;
import com.logistics.routing.application.routing.DistanceMatrix;
import com.logistics.routing.application.routing.GeoCoordinate;
import com.logistics.routing.domain.route.exception.RoutingEngineException;
import com.logistics.routing.domain.route.exception.RoutingEngineUnavailableException;
import com.logistics.routing.domain.route.model.enums.TransportType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableWireMock({
        @ConfigureWireMock(
                name = "osrm",
                baseUrlProperties = {
                        "routing.osrm-base-urls.TRUCK",
                        "routing.osrm-base-urls.TRAIN",
                        "routing.osrm-base-urls.VESSEL"
                }
        )
})
class OsrmRetryAndCircuitBreakerIntegrationTest {

    private static final String TABLE_PATH = "/table/v1/.*";
    private static final String TRUCK_CB = OsrmRoutingClient.resilienceInstanceName(TransportType.TRUCK);
    private static final String TRAIN_CB = OsrmRoutingClient.resilienceInstanceName(TransportType.TRAIN);
    private static final String OK_TABLE_BODY = """
            {
              "code": "Ok",
              "durations": [[0.0]],
              "distances": [[0.0]]
            }
            """;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.5")
                    .asCompatibleSubstituteFor("postgres")
    );

    @InjectWireMock("osrm")
    private WireMockServer osrm;

    @Autowired
    @Qualifier("truckOsrmClient")
    private RoutingClient truckOsrmClient;

    @Autowired
    @Qualifier("trainOsrmClient")
    private RoutingClient trainOsrmClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetState() {
        osrm.resetAll();
        circuitBreakerRegistry.circuitBreaker(TRUCK_CB).reset();
        circuitBreakerRegistry.circuitBreaker(TRAIN_CB).reset();
        circuitBreakerRegistry.circuitBreaker(OsrmRoutingClient.resilienceInstanceName(TransportType.VESSEL)).reset();
    }

    @Nested
    class RetryOnNetworkFailure {

        @Test
        void retriesConnectivityFaultsAndReturnsMatrixOnLaterSuccess() {
            osrm.stubFor(post(urlPathMatching(TABLE_PATH))
                    .inScenario("recover")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                    .willSetStateTo("second-attempt"));

            osrm.stubFor(post(urlPathMatching(TABLE_PATH))
                    .inScenario("recover")
                    .whenScenarioStateIs("second-attempt")
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                    .willSetStateTo("third-attempt"));

            osrm.stubFor(post(urlPathMatching(TABLE_PATH))
                    .inScenario("recover")
                    .whenScenarioStateIs("third-attempt")
                    .willReturn(okTableResponse()));

            DistanceMatrix result = truckOsrmClient.getDistanceMatrix(singlePoint());

            assertThat(result.distancesMeters()).hasSize(1);
            assertThat(result.distancesMeters().getFirst().getFirst())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            osrm.verify(exactly(3), postRequestedFor(urlPathMatching(TABLE_PATH)));
        }

        @Test
        void exhaustsRetriesAndThrowsUnavailableWhenNetworkKeepsFailing() {
            osrm.stubFor(post(urlPathMatching(TABLE_PATH))
                    .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

            assertThatThrownBy(() -> truckOsrmClient.getDistanceMatrix(singlePoint()))
                    .isInstanceOf(RoutingEngineUnavailableException.class)
                    .hasMessage("OSRM service unavailable");

            osrm.verify(exactly(3), postRequestedFor(urlPathMatching(TABLE_PATH)));
        }
    }

    @Nested
    class NoRetryOnHttpError {

        @Test
        void doesNotRetryClientErrorResponses() {
            osrm.stubFor(post(urlPathMatching(TABLE_PATH))
                    .willReturn(aResponse().withStatus(400).withBody("bad request")));

            assertThatThrownBy(() -> truckOsrmClient.getDistanceMatrix(singlePoint()))
                    .isInstanceOf(RoutingEngineException.class)
                    .hasMessage("OSRM request failed with HTTP 400");

            osrm.verify(exactly(1), postRequestedFor(urlPathMatching(TABLE_PATH)));
        }

        @Test
        void doesNotRetryServerErrorResponses() {
            osrm.stubFor(post(urlPathMatching(TABLE_PATH))
                    .willReturn(aResponse().withStatus(503).withBody("unavailable")));

            assertThatThrownBy(() -> truckOsrmClient.getDistanceMatrix(singlePoint()))
                    .isInstanceOf(RoutingEngineException.class)
                    .hasMessage("OSRM request failed with HTTP 503");

            osrm.verify(exactly(1), postRequestedFor(urlPathMatching(TABLE_PATH)));
        }
    }

    @Nested
    class CircuitBreakerOpens {

        @Test
        void opensAfterRepeatedFailuresAndMapsBlockedCallsToUnavailable() {
            osrm.stubFor(post(urlPathMatching(TABLE_PATH))
                    .willReturn(aResponse().withStatus(500).withBody("error")));

            for (int attempt = 1; attempt <= 4; attempt++) {
                assertThatThrownBy(() -> truckOsrmClient.getDistanceMatrix(singlePoint()))
                        .isInstanceOf(RoutingEngineException.class);
            }

            assertThat(circuitBreakerRegistry.circuitBreaker(TRUCK_CB).getState())
                    .isEqualTo(CircuitBreaker.State.OPEN);

            assertThatThrownBy(() -> truckOsrmClient.getDistanceMatrix(singlePoint()))
                    .isInstanceOf(RoutingEngineUnavailableException.class)
                    .hasMessage("OSRM service unavailable");

            osrm.verify(exactly(4), postRequestedFor(urlPathMatching(TABLE_PATH)));
        }

        @Test
        void doesNotOpenTrainBreakerWhenTruckBreakerOpens() {
            osrm.stubFor(post(urlPathMatching(TABLE_PATH))
                    .willReturn(aResponse().withStatus(500).withBody("error")));

            for (int attempt = 1; attempt <= 4; attempt++) {
                assertThatThrownBy(() -> truckOsrmClient.getDistanceMatrix(singlePoint()))
                        .isInstanceOf(RoutingEngineException.class);
            }

            assertThat(circuitBreakerRegistry.circuitBreaker(TRUCK_CB).getState())
                    .isEqualTo(CircuitBreaker.State.OPEN);
            assertThat(circuitBreakerRegistry.circuitBreaker(TRAIN_CB).getState())
                    .isEqualTo(CircuitBreaker.State.CLOSED);

            osrm.resetAll();
            osrm.stubFor(post(urlPathMatching(TABLE_PATH)).willReturn(okTableResponse()));

            DistanceMatrix result = trainOsrmClient.getDistanceMatrix(singlePoint());

            assertThat(result.distancesMeters()).hasSize(1);
            osrm.verify(exactly(1), postRequestedFor(urlPathMatching(TABLE_PATH)));
        }
    }

    private static List<GeoCoordinate> singlePoint() {
        return List.of(new GeoCoordinate(BigDecimal.valueOf(55), BigDecimal.valueOf(37)));
    }

    private static ResponseDefinitionBuilder okTableResponse() {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(OK_TABLE_BODY);
    }
}
