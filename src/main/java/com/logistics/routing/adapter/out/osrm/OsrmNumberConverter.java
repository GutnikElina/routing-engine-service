package com.logistics.routing.adapter.out.osrm;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;

@UtilityClass
class OsrmNumberConverter {

    double toOsrmDouble(BigDecimal value) {
        return value.doubleValue();
    }

    BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    List<List<BigDecimal>> toBigDecimalMatrix(List<List<Double>> matrix) {
        if (matrix == null) {
            return null;
        }

        return matrix.stream()
                .map(OsrmNumberConverter::toBigDecimalRow)
                .toList();
    }

    private List<BigDecimal> toBigDecimalRow(List<Double> row) {
        if (row == null) {
            return null;
        }

        return row.stream()
                .map(OsrmNumberConverter::toBigDecimal)
                .toList();
    }
}
