package com.logistics.routing.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "geofence")
public class GeofenceProperties {

    @NotNull
    @DecimalMin(value = "1", message = "default-radius-meters must be greater than zero")
    private BigDecimal defaultRadiusMeters = new BigDecimal("150");
}
