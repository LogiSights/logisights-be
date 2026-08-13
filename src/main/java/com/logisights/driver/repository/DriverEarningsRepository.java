package com.logisights.driver.repository;

import com.logisights.driver.entity.DriverEarningsEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DriverEarningsRepository implements PanacheRepositoryBase<DriverEarningsEntity, UUID> {

    public List<DriverEarningsEntity> findByDriver(UUID driverId) {
        return find("driverId", Sort.descending("createdAt"), driverId).list();
    }

    public BigDecimal totalForDriver(UUID driverId) {
        return find("driverId", driverId).stream()
                .map(e -> e.amountKes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
