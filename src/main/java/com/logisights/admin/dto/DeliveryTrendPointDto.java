package com.logisights.admin.dto;

import java.time.LocalDate;

public record DeliveryTrendPointDto(LocalDate day, long deliveries) {
}
