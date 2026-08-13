package com.logisights.pickup.repository;

import com.logisights.pickup.entity.PickupPointEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PickupPointRepository implements PanacheRepositoryBase<PickupPointEntity, UUID> {

    public List<PickupPointEntity> findByStaff(UUID staffUserId) {
        return list("staffUserId", staffUserId);
    }
}
