package com.logisights.pickup.service;

import com.logisights.common.PickupActivityAction;
import com.logisights.common.PickupItemStatus;
import com.logisights.pickup.dto.PickupActivityDto;
import com.logisights.pickup.dto.PickupInventoryDto;
import com.logisights.pickup.dto.RecordActivityRequest;
import com.logisights.pickup.entity.PickupActivityLogEntity;
import com.logisights.pickup.entity.PickupInventoryEntity;
import com.logisights.pickup.repository.PickupActivityLogRepository;
import com.logisights.pickup.repository.PickupInventoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class PickupService {

    @Inject
    PickupInventoryRepository inventoryRepository;

    @Inject
    PickupActivityLogRepository activityLogRepository;

    public List<PickupInventoryDto> inventoryFor(UUID pickupPointId) {
        return inventoryRepository.findByPickupPoint(pickupPointId).stream()
                .map(PickupInventoryDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PickupActivityDto recordActivity(UUID staffUserId, RecordActivityRequest request) {
        PickupActivityLogEntity log = new PickupActivityLogEntity();
        log.pickupPointId = request.pickupPointId();
        log.staffUserId = staffUserId;
        log.parcelId = request.parcelId();
        log.action = request.action();
        log.type = request.type();
        activityLogRepository.persist(log);

        if (request.action() == PickupActivityAction.CHECK_OUT) {
            inventoryRepository.find("parcelId", request.parcelId()).firstResultOptional()
                    .ifPresent(item -> item.status = PickupItemStatus.PICKED_UP);
        }

        return PickupActivityDto.from(log);
    }
}
