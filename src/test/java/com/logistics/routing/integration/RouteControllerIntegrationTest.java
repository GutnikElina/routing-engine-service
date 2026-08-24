package com.logistics.routing.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.routing.RoutingEngineApplicationTests;
import com.logistics.routing.adapter.in.web.generated.model.CreateRouteResponse;
import com.logistics.routing.testdata.CreateRouteTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RoutingEngineApplicationTests.class)
class RouteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE routing.route_orders CASCADE");
    }

    @Nested
    class CreateRoute {

        @Test
        void createsRouteAndPersistsWaypoints() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/routes")
                            .contentType(APPLICATION_JSON)
                            .content(CreateRouteTestData.validCreateRouteRequestJson()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderNumber").value("ORDER-1001"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.routeId").isNotEmpty())
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andReturn();

            CreateRouteResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsByteArray(),
                    CreateRouteResponse.class
            );
            UUID routeId = response.getRouteId();

            Map<String, Object> route = jdbcTemplate.queryForMap(
                    """
                    SELECT id, order_number, status, adr_class
                    FROM routing.route_orders
                    WHERE id = ?
                    """,
                    routeId
            );
            List<Map<String, Object>> waypoints = jdbcTemplate.queryForList(
                    """
                    SELECT sequence_number,
                           waypoint_type,
                           ST_Y(location) AS latitude,
                           ST_X(location) AS longitude,
                           address
                    FROM routing.waypoints
                    WHERE route_order_id = ?
                    ORDER BY sequence_number
                    """,
                    routeId
            );

            assertThat(route)
                    .containsEntry("order_number", "ORDER-1001")
                    .containsEntry("status", "DRAFT")
                    .containsEntry("adr_class", "4.1");
            assertThat(waypoints).hasSize(2);
            assertThat(waypoints.getFirst())
                    .containsEntry("sequence_number", 1)
                    .containsEntry("waypoint_type", "ORIGIN")
                    .containsEntry("address", "Minsk");
            assertThat((Double) waypoints.getFirst().get("latitude")).isCloseTo(53.9006, offset(0.000001));
            assertThat((Double) waypoints.getFirst().get("longitude")).isCloseTo(27.5590, offset(0.000001));
            assertThat(waypoints.getLast())
                    .containsEntry("sequence_number", 2)
                    .containsEntry("waypoint_type", "DESTINATION")
                    .containsEntry("address", "Warsaw");
            assertThat((Double) waypoints.getLast().get("latitude")).isCloseTo(52.2297, offset(0.000001));
            assertThat((Double) waypoints.getLast().get("longitude")).isCloseTo(21.0122, offset(0.000001));
        }

        @Test
        void returnsBadRequestWhenRequestViolatesContractValidation() throws Exception {
            mockMvc.perform(post("/api/v1/routes")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "cargo": {},
                                      "waypoints": []
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid request"))
                    .andExpect(jsonPath("$.errors").isArray());
        }
    }

}
