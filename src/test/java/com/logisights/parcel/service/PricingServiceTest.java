package com.logisights.parcel.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private PricingService newService() {
        PricingService service = new PricingService();
        service.baseCostKes = new BigDecimal("200");
        service.costPerKgKes = new BigDecimal("50");
        return service;
    }

    @Test
    void computesCostAsBasePlusPerKg() {
        PricingService service = newService();

        BigDecimal cost = service.computeCost(new BigDecimal("3.5"));

        assertThat(cost).isEqualByComparingTo("375.00");
    }

    @Test
    void roundsToTwoDecimalPlaces() {
        PricingService service = newService();

        BigDecimal cost = service.computeCost(new BigDecimal("0.333"));

        assertThat(cost.scale()).isEqualTo(2);
        assertThat(cost).isEqualByComparingTo("216.65");
    }

    @Test
    void zeroWeightStillChargesBaseCost() {
        PricingService service = newService();

        BigDecimal cost = service.computeCost(BigDecimal.ZERO);

        assertThat(cost).isEqualByComparingTo("200.00");
    }
}
