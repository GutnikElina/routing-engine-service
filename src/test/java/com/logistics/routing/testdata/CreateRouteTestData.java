package com.logistics.routing.testdata;

import com.logistics.routing.application.route.create.CargoCommand;
import com.logistics.routing.application.route.create.CreateRouteCommand;
import com.logistics.routing.application.route.create.WaypointCommand;
import com.logistics.routing.domain.route.model.enums.WaypointType;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;

@UtilityClass
public class CreateRouteTestData {

    public static final String ORDER_NUMBER = "ORDER-123";

    public static CreateRouteCommand validCreateRouteCommand() {
        return new CreateRouteCommand(
                ORDER_NUMBER,
                new CargoCommand(
                        new BigDecimal("100.500"),
                        new BigDecimal("2.250"),
                        "3",
                        new BigDecimal("-10.00"),
                        new BigDecimal("20.00")
                ),
                List.of(
                        waypoint(WaypointType.ORIGIN, 1),
                        waypoint(WaypointType.DESTINATION, 2)
                )
        );
    }

    public static CreateRouteCommand commandWithInvalidFirstWaypoint() {
        return new CreateRouteCommand(
                ORDER_NUMBER,
                validCreateRouteCommand().cargo(),
                List.of(
                        waypoint(WaypointType.DESTINATION, 1),
                        waypoint(WaypointType.ORIGIN, 2)
                )
        );
    }

    public static String validCreateRouteRequestJson() {
        return """
                {
                  "orderNumber": "ORDER-1001",
                  "cargo": {
                    "weightKg": 1250.500,
                    "volumeM3": 8.750,
                    "adrClass": "4.1",
                    "temperatureMin": -10.00,
                    "temperatureMax": 5.00
                  },
                  "waypoints": [
                    {
                      "type": "ORIGIN",
                      "sequence": 1,
                      "latitude": 53.9006,
                      "longitude": 27.5590,
                      "address": "Minsk",
                      "timeWindowStart": "2026-08-25T08:00:00Z",
                      "timeWindowEnd": "2026-08-25T10:00:00Z"
                    },
                    {
                      "type": "DESTINATION",
                      "sequence": 2,
                      "latitude": 52.2297,
                      "longitude": 21.0122,
                      "address": "Warsaw",
                      "timeWindowStart": "2026-08-26T08:00:00Z",
                      "timeWindowEnd": "2026-08-26T10:00:00Z"
                    }
                  ]
                }
                """;
    }

    private static WaypointCommand waypoint(WaypointType type, int sequence) {
        return new WaypointCommand(
                type,
                sequence,
                new BigDecimal("53.900000"),
                new BigDecimal("27.566700"),
                "Minsk",
                null,
                null
        );
    }
}
