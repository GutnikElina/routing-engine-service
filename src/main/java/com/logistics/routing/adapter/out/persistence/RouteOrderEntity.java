package com.logistics.routing.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "route_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteOrderEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    private String orderNumber;

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private RouteOrderStatus status;

    @Column(name = "cargo_weight_kg", precision = 12, scale = 3)
    private BigDecimal cargoWeightKg;

    @Column(name = "cargo_volume_m3", precision = 12, scale = 3)
    private BigDecimal cargoVolumeM3;

    @Column(name = "adr_class", length = 16)
    private AdrClass adrClass;

    @Column(name = "temperature_min", precision = 5, scale = 2)
    private BigDecimal temperatureMin;

    @Column(name = "temperature_max", precision = 5, scale = 2)
    private BigDecimal temperatureMax;

    @Column(name = "total_distance_km", precision = 12, scale = 3)
    private BigDecimal totalDistanceKm;

    @Column(name = "total_cost", precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "eta")
    private Instant eta;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;
}
