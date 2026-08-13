package com.logisights.pickup.repository;

import com.logisights.pickup.entity.PickupInventoryEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PickupInventoryRepository implements PanacheRepositoryBase<PickupInventoryEntity, UUID> {

    public List<PickupInventoryEntity> findByPickupPoint(UUID pickupPointId) {
        return find("pickupPointId", Sort.descending("dateArrived"), pickupPointId).list();
    }
}
