package com.logisights.driver.service;

import com.logisights.driver.dto.DriverEarningsSummaryDto;
import com.logisights.driver.entity.DriverEarningsEntity;
import com.logisights.driver.repository.DriverEarningsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    DriverEarningsRepository driverEarningsRepository;

    DriverService driverService;

    @BeforeEach
    void setUp() {
        driverService = new DriverService();
        driverService.driverEarningsRepository = driverEarningsRepository;
    }

    @Test
    void earningsSummaryAggregatesCountAndTotal() {
        UUID driverId = UUID.randomUUID();
        DriverEarningsEntity e1 = new DriverEarningsEntity();
        DriverEarningsEntity e2 = new DriverEarningsEntity();
        when(driverEarningsRepository.findByDriver(driverId)).thenReturn(List.of(e1, e2));
        when(driverEarningsRepository.totalForDriver(driverId)).thenReturn(new BigDecimal("300.00"));

        DriverEarningsSummaryDto result = driverService.earningsSummary(driverId);

        assertThat(result.driverId()).isEqualTo(driverId);
        assertThat(result.completedDeliveries()).isEqualTo(2);
        assertThat(result.totalEarningsKes()).isEqualByComparingTo("300.00");
    }
}
