package com.logistics.routing.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routing.adapter.in.web.generated.model.CreateRouteResponse;
import com.logistics.routing.testdata.CreateRouteTestData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RouteControllerIntegrationTest {

    private static final String ROUTES_PATH = "/api/v1/routes";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.5")
                    .asCompatibleSubstituteFor("postgres")
    );

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class CreateRoute {

        @Test
        void createsRoute() {
            ResponseEntity<CreateRouteResponse> response = restTemplate.postForEntity(
                    ROUTES_PATH,
                    jsonRequest(CreateRouteTestData.validCreateRouteRequestJson()),
                    CreateRouteResponse.class
            );

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getHeaders().getContentType().isCompatibleWith(APPLICATION_JSON)).isTrue();
            CreateRouteResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getOrderNumber()).startsWith("ORDER-");
            assertThat(body.getStatus()).isEqualTo("DRAFT");
            assertThat(body.getRouteId()).isNotNull();
            assertThat(body.getCreatedAt()).isNotNull();
        }

        @Test
        void returnsConflictWhenOrderNumberAlreadyExists() throws Exception {
            String request = CreateRouteTestData.validCreateRouteRequestJson();

            ResponseEntity<CreateRouteResponse> initialResponse = restTemplate.postForEntity(
                    ROUTES_PATH,
                    jsonRequest(request),
                    CreateRouteResponse.class
            );
            ResponseEntity<String> response = restTemplate.postForEntity(
                    ROUTES_PATH,
                    jsonRequest(request),
                    String.class
            );

            assertThat(initialResponse.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getHeaders().getContentType().isCompatibleWith(APPLICATION_PROBLEM_JSON)).isTrue();
            assertThat(objectMapper.readTree(response.getBody()).path("title").asText()).isEqualTo("Duplicate order number");
            assertThat(objectMapper.readTree(response.getBody()).path("detail").asText())
                    .isEqualTo("A route with this order number already exists");
        }

        @Test
        void returnsBadRequestWhenRouteDraftIsInvalid() throws Exception {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    ROUTES_PATH,
                    jsonRequest(CreateRouteTestData.invalidFirstWaypointCreateRouteRequestJson()),
                    String.class
            );

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getHeaders().getContentType().isCompatibleWith(APPLICATION_PROBLEM_JSON)).isTrue();
            assertThat(objectMapper.readTree(response.getBody()).path("title").asText()).isEqualTo("Invalid route draft");
            assertThat(objectMapper.readTree(response.getBody()).path("detail").asText())
                    .isEqualTo("the first waypoint must be an ORIGIN");
        }

        @Test
        void returnsBadRequestWhenRequestViolatesContractValidation() throws Exception {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    ROUTES_PATH,
                    jsonRequest(CreateRouteTestData.invalidContractCreateRouteRequestJson()),
                    String.class
            );

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getHeaders().getContentType().isCompatibleWith(APPLICATION_PROBLEM_JSON)).isTrue();
            assertThat(objectMapper.readTree(response.getBody()).path("title").asText()).isEqualTo("Invalid request");
            assertThat(objectMapper.readTree(response.getBody()).path("errors").isArray()).isTrue();
        }

        private HttpEntity<String> jsonRequest(String body) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(APPLICATION_JSON);
            return new HttpEntity<>(body, headers);
        }
    }

}
