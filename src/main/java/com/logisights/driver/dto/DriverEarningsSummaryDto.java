package com.logisights.driver.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DriverEarningsSummaryDto(
        UUID driverId,
        BigDecimal totalEarningsKes,
        long completedDeliveries
) {
}
