package com.logisights.pickup.repository;

import com.logisights.pickup.entity.PickupActivityLogEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PickupActivityLogRepository implements PanacheRepositoryBase<PickupActivityLogEntity, UUID> {

    public List<PickupActivityLogEntity> findByPickupPoint(UUID pickupPointId) {
        return find("pickupPointId", Sort.descending("createdAt"), pickupPointId).list();
    }
}
