package com.logisights.driver.service;

import com.logisights.driver.dto.DriverEarningsSummaryDto;
import com.logisights.driver.repository.DriverEarningsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class DriverService {

    @Inject
    DriverEarningsRepository driverEarningsRepository;

    public DriverEarningsSummaryDto earningsSummary(UUID driverId) {
        var earnings = driverEarningsRepository.findByDriver(driverId);
        var total = driverEarningsRepository.totalForDriver(driverId);
        return new DriverEarningsSummaryDto(driverId, total, earnings.size());
    }
}
