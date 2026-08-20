CREATE SCHEMA IF NOT EXISTS routing;
SET search_path TO routing, public;

CREATE TABLE route_orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL CHECK (status IN ('CREATED', 'PLANNING', 'PLANNED', 'IN_TRANSIT', 'COMPLETED', 'CLOSED', 'CANCELLED')),
    cargo_weight_kg NUMERIC(12, 3),
    cargo_volume_m3 NUMERIC(12, 3),
    adr_class VARCHAR(16) CHECK (adr_class IN ('1', '2', '3', '4.1', '4.2', '4.3', '5.1', '5.2', '6.1', '6.2', '7', '8', '9', 'X')),
    temperature_min NUMERIC(5, 2),
    temperature_max NUMERIC(5, 2),
    total_distance_km NUMERIC(12, 3),
    total_cost DECIMAL(12, 2),
    eta TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE route_segments (
    id UUID PRIMARY KEY,
    route_order_id UUID NOT NULL REFERENCES route_orders(id) ON DELETE CASCADE,
    segment_index INT NOT NULL,
    transport_type VARCHAR(32) NOT NULL CHECK (transport_type IN ('TRUCK', 'TRAIN', 'VESSEL', 'PLANE')),
    planned_start_time TIMESTAMP WITH TIME ZONE,
    planned_end_time TIMESTAMP WITH TIME ZONE,
    actual_start_time TIMESTAMP WITH TIME ZONE,
    actual_end_time TIMESTAMP WITH TIME ZONE,
    path_geometry GEOMETRY(LineString, 4326),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE waypoints (
    id UUID PRIMARY KEY,
    route_order_id UUID NOT NULL REFERENCES route_orders(id) ON DELETE CASCADE,
    waypoint_type VARCHAR(32) NOT NULL CHECK (waypoint_type IN ('PICKUP', 'DELIVERY', 'TRANSFER', 'WAREHOUSE')),
    sequence_number INT NOT NULL,
    location GEOMETRY(Point, 4326) NOT NULL,
    address VARCHAR(255),
    time_window_start TIMESTAMP WITH TIME ZONE,
    time_window_end TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_route_segments_route_order ON route_segments(route_order_id);
CREATE INDEX idx_waypoints_route_order ON waypoints(route_order_id);
CREATE INDEX idx_outbox_events_pending ON outbox_events(status, created_at) WHERE status = 'PENDING';

CREATE INDEX idx_route_segments_geometry ON route_segments USING GIST (path_geometry);
CREATE INDEX idx_waypoints_location ON waypoints USING GIST (location);