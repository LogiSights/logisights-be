package com.logisights.admin.dto;

import java.math.BigDecimal;

public record AdminStatsSummaryDto(
        long totalUsers,
        long activeDrivers,
        long parcelsToday,
        BigDecimal revenueToday
) {
}
