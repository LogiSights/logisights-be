package com.logisights.pickup.service;

import com.logisights.common.ApiException;
import com.logisights.common.PickupActivityAction;
import com.logisights.common.PickupItemStatus;
import com.logisights.notification.MailSender;
import com.logisights.parcel.entity.ParcelEntity;
import com.logisights.parcel.repository.ParcelRepository;
import com.logisights.pickup.dto.PickupActivityDto;
import com.logisights.pickup.dto.RecordActivityRequest;
import com.logisights.pickup.entity.PickupInventoryEntity;
import com.logisights.pickup.entity.PickupPointEntity;
import com.logisights.pickup.repository.PickupActivityLogRepository;
import com.logisights.pickup.repository.PickupInventoryRepository;
import com.logisights.pickup.repository.PickupPointRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickupServiceTest {

    @Mock
    PickupInventoryRepository inventoryRepository;
    @Mock
    PickupActivityLogRepository activityLogRepository;
    @Mock
    PickupPointRepository pickupPointRepository;
    @Mock
    ParcelRepository parcelRepository;
    @Mock
    MailSender mailSender;
    @Mock
    PanacheQuery<PickupInventoryEntity> panacheQuery;

    PickupService pickupService;

    @BeforeEach
    void setUp() {
        pickupService = new PickupService();
        pickupService.inventoryRepository = inventoryRepository;
        pickupService.activityLogRepository = activityLogRepository;
        pickupService.pickupPointRepository = pickupPointRepository;
        pickupService.parcelRepository = parcelRepository;
        pickupService.mailSender = mailSender;
    }

    @Test
    void checkInWithoutRecipientEmailSkipsNotification() {
        UUID parcelId = UUID.randomUUID();
        UUID pickupPointId = UUID.randomUUID();
        ParcelEntity parcel = new ParcelEntity();
        parcel.recipientEmail = null;
        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));

        RecordActivityRequest request = new RecordActivityRequest(parcelId, pickupPointId,
                PickupActivityAction.CHECK_IN, "arrival");

        PickupActivityDto result = pickupService.recordActivity(UUID.randomUUID(), request);

        assertThat(result.action()).isEqualTo(PickupActivityAction.CHECK_IN);
        verify(inventoryRepository).persist(any(PickupInventoryEntity.class));
        verifyNoInteractions(mailSender);
    }

    @Test
    void checkInWithRecipientEmailSendsPickupReadyNotification() {
        UUID parcelId = UUID.randomUUID();
        UUID pickupPointId = UUID.randomUUID();
        ParcelEntity parcel = new ParcelEntity();
        parcel.recipientEmail = "bob@example.com";
        parcel.recipientName = "Bob";
        parcel.trackingId = "LGS-ABC123";
        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));

        PickupPointEntity pickupPoint = new PickupPointEntity();
        pickupPoint.name = "Nairobi CBD";
        pickupPoint.address = "Moi Ave";
        when(pickupPointRepository.findByIdOptional(pickupPointId)).thenReturn(Optional.of(pickupPoint));

        RecordActivityRequest request = new RecordActivityRequest(parcelId, pickupPointId,
                PickupActivityAction.CHECK_IN, "arrival");

        pickupService.recordActivity(UUID.randomUUID(), request);

        verify(mailSender).sendPickupReady("bob@example.com", "Bob", "LGS-ABC123", "Nairobi CBD", "Moi Ave");
    }

    @Test
    void checkInThrowsWhenParcelMissing() {
        UUID parcelId = UUID.randomUUID();
        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.empty());

        RecordActivityRequest request = new RecordActivityRequest(parcelId, UUID.randomUUID(),
                PickupActivityAction.CHECK_IN, "arrival");

        assertThatThrownBy(() -> pickupService.recordActivity(UUID.randomUUID(), request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void checkOutMarksInventoryItemPickedUp() {
        UUID parcelId = UUID.randomUUID();
        PickupInventoryEntity inventoryItem = new PickupInventoryEntity();
        inventoryItem.status = PickupItemStatus.AWAITING_PICKUP;

        when(inventoryRepository.find("parcelId", parcelId)).thenReturn(panacheQuery);
        when(panacheQuery.firstResultOptional()).thenReturn(Optional.of(inventoryItem));

        RecordActivityRequest request = new RecordActivityRequest(parcelId, UUID.randomUUID(),
                PickupActivityAction.CHECK_OUT, "collected");

        pickupService.recordActivity(UUID.randomUUID(), request);

        assertThat(inventoryItem.status).isEqualTo(PickupItemStatus.PICKED_UP);
    }
}
