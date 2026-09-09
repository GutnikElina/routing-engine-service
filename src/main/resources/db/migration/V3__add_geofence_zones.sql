SET search_path TO routing, public;

CREATE TABLE geofence_zones (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    zone_type VARCHAR(32) NOT NULL CHECK (zone_type IN ('WAREHOUSE', 'PORT', 'FUEL_STATION', 'HAZARD')),
    source_waypoint_id UUID REFERENCES waypoints(id) ON DELETE SET NULL,
    polygon GEOMETRY(Polygon, 4326) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_geofence_zones_source_waypoint ON geofence_zones(source_waypoint_id);
CREATE INDEX idx_geofence_zones_polygon ON geofence_zones USING GIST (polygon);
