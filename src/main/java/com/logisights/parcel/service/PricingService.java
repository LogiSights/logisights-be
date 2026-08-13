package com.logisights.parcel.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single source of truth for parcel cost — mirrors the formula the frontend
 * used to compute client-side (BASE_COST_KSH + COST_PER_KG_KSH * weight),
 * now server-authoritative so a tampered client can't under-quote a delivery.
 */
@ApplicationScoped
public class PricingService {

    @ConfigProperty(name = "pricing.base-cost-kes")
    BigDecimal baseCostKes;

    @ConfigProperty(name = "pricing.cost-per-kg-kes")
    BigDecimal costPerKgKes;

    public BigDecimal computeCost(BigDecimal weightKg) {
        return baseCostKes.add(costPerKgKes.multiply(weightKg))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
