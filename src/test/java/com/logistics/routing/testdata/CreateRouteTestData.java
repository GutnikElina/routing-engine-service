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

    public final String ORDER_NUMBER = "ORDER-123";

    public CreateRouteCommand validCreateRouteCommand() {
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
                        waypoint(WaypointType.PICKUP, 1),
                        waypoint(WaypointType.DELIVERY, 2)
                )
        );
    }

    public CreateRouteCommand commandWithInvalidFirstWaypoint() {
        return new CreateRouteCommand(
                ORDER_NUMBER,
                validCreateRouteCommand().cargo(),
                List.of(
                        waypoint(WaypointType.DELIVERY, 1),
                        waypoint(WaypointType.PICKUP, 2)
                )
        );
    }

    private WaypointCommand waypoint(WaypointType type, int sequence) {
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
