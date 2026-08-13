package com.logisights.admin.dto;

import com.logisights.common.ParcelStatus;

public record StatusBreakdownDto(ParcelStatus status, long count) {
}
