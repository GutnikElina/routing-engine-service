package com.logistics.routing.adapter.in.web.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CargoRequest(
    @Positive
    @Digits(integer = 9, fraction = 3)
    BigDecimal weightKg,

    @Positive
    @Digits(integer = 9, fraction = 3)
    BigDecimal volumeM3,

    @Pattern(
        regexp = "^(1|2|3|4\\.1|4\\.2|4\\.3|5\\.1|5\\.2|6\\.1|6\\.2|7|8|9|X)$",
        message = "must be a valid ADR class"
    )
    String adrClass,

    @Digits(integer = 3, fraction = 2)
    BigDecimal temperatureMin,

    @Digits(integer = 3, fraction = 2)
    BigDecimal temperatureMax
) {
}
