package com.logistics.routing.testdata;

import com.logistics.routing.application.route.create.CargoCommand;
import com.logistics.routing.application.route.create.CreateRouteCommand;
import com.logistics.routing.application.route.create.WaypointCommand;
import com.logistics.routing.domain.route.model.enums.AdrClass;
import com.logistics.routing.domain.route.model.enums.WaypointType;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class CreateRouteTestData {

    public static final String ORDER_NUMBER = randomOrderNumber();

    public static CreateRouteCommand validCreateRouteCommand() {
        return new CreateRouteCommand(
                ORDER_NUMBER,
                new CargoCommand(
                        randomDecimal(100, 10_000, 3),
                        randomDecimal(1, 100, 3),
                        randomAdrClass(),
                        randomDecimal(-30, 0, 2),
                        randomDecimal(1, 30, 2)
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
        return validCreateRouteRequestJson(randomOrderNumber());
    }

    public static String invalidFirstWaypointCreateRouteRequestJson() {
        return validCreateRouteRequestJson()
                .replaceFirst("\"type\": \"ORIGIN\"", "\"type\": \"DESTINATION\"");
    }

    public static String invalidContractCreateRouteRequestJson() {
        return """
                {
                  "cargo": {},
                  "waypoints": []
                }
                """;
    }

    private static String validCreateRouteRequestJson(String orderNumber) {
        return """
                {
                  "orderNumber": "%s",
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
                """.formatted(orderNumber);
    }

    private static String randomOrderNumber() {
        return "ORDER-" + ThreadLocalRandom.current().nextLong(1_000_000, 10_000_000);
    }

    private static WaypointCommand waypoint(WaypointType type, int sequence) {
        return new WaypointCommand(
                type,
                sequence,
                randomDecimal(45, 55, 6),
                randomDecimal(20, 30, 6),
                "Test address " + ThreadLocalRandom.current().nextLong(1_000_000, 10_000_000),
                null,
                null
        );
    }

    private static String randomAdrClass() {
        AdrClass[] adrClasses = AdrClass.values();
        return adrClasses[ThreadLocalRandom.current().nextInt(adrClasses.length)].code();
    }

    private static BigDecimal randomDecimal(int origin, int bound, int scale) {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(origin, bound))
                .setScale(scale, RoundingMode.HALF_UP);
    }
}
