package com.logistics.routing.application.route.create;

import java.math.BigDecimal;

public record CargoCommand(
    BigDecimal weightKg,
    BigDecimal volumeM3,
    String adrClass,
    BigDecimal temperatureMin,
    BigDecimal temperatureMax
) {
    
}
