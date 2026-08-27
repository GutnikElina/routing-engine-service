package com.logistics.routing.application.route.create;

import java.util.List;

public record CreateRouteCommand(
    String orderNumber,
    CargoCommand cargo,
    List<WaypointCommand> waypoints
) {

}
