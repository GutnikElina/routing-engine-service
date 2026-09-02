package com.logistics.routing.adapter.out.routing;

import com.logistics.routing.domain.route.model.enums.TransportType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "routing")
public class RoutingProperties {

    @Min(2)
    @Max(1000)
    private int maxCoordinates = 100;

    @Pattern(regexp = "^(simplified|full)$", message = "route-overview must be simplified or full")
    private String routeOverview = "simplified";

    private Duration connectTimeout = Duration.ofSeconds(2);

    private Duration readTimeout = Duration.ofSeconds(15);

    @NotEmpty
    private Map<TransportType, String> osrmBaseUrls;

    @NotNull
    @DecimalMin(value = "0.01", message = "plane-average-speed-kmh must be greater than zero")
    private BigDecimal planeAverageSpeedKmh = new BigDecimal("800");
}
