package com.logisights.parcel.repository;

import com.logisights.common.ParcelStatus;
import com.logisights.parcel.entity.ParcelEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ParcelRepository implements PanacheRepositoryBase<ParcelEntity, UUID> {

    public Optional<ParcelEntity> findByTrackingId(String trackingId) {
        return find("trackingId", trackingId).firstResultOptional();
    }

    public List<ParcelEntity> findBySender(UUID senderId) {
        return find("senderId", Sort.descending("createdAt"), senderId).list();
    }

    public List<ParcelEntity> findByDriver(UUID driverId) {
        return find("driverId", Sort.descending("createdAt"), driverId).list();
    }

    public List<ParcelEntity> findByDriverAndStatus(UUID driverId, ParcelStatus status) {
        return find("driverId = ?1 and status = ?2", Sort.descending("createdAt"), driverId, status).list();
    }

    public boolean existsByTrackingId(String trackingId) {
        return count("trackingId", trackingId) > 0;
    }
}
