package com.logistics.routing.application.port.out;

import com.logistics.routing.application.geofence.generate.GeofenceZoneCreatedEvent;
import com.logistics.routing.application.route.create.RouteCreatedEvent;

public interface OutboxEventPort {
    void append(RouteCreatedEvent event);

    void append(GeofenceZoneCreatedEvent event);
}
