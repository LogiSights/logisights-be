package com.logisights.parcel.repository;

import com.logisights.parcel.entity.ParcelStatusHistoryEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ParcelStatusHistoryRepository implements PanacheRepositoryBase<ParcelStatusHistoryEntity, UUID> {

    public List<ParcelStatusHistoryEntity> findByParcel(UUID parcelId) {
        return find("parcelId", Sort.ascending("createdAt"), parcelId).list();
    }
}
