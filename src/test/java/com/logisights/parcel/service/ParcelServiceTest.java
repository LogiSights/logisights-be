package com.logisights.parcel.service;

import com.logisights.auth.entity.UserEntity;
import com.logisights.auth.repository.UserRepository;
import com.logisights.common.ApiException;
import com.logisights.common.ParcelCity;
import com.logisights.common.ParcelStatus;
import com.logisights.common.UserRole;
import com.logisights.driver.repository.DriverEarningsRepository;
import com.logisights.notification.MailSender;
import com.logisights.parcel.dto.BookParcelRequest;
import com.logisights.parcel.dto.ParcelDto;
import com.logisights.parcel.dto.UpdateStatusRequest;
import com.logisights.parcel.entity.ParcelEntity;
import com.logisights.parcel.repository.ParcelRepository;
import com.logisights.parcel.repository.ParcelStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParcelServiceTest {

    @Mock
    ParcelRepository parcelRepository;
    @Mock
    ParcelStatusHistoryRepository historyRepository;
    @Mock
    DriverEarningsRepository driverEarningsRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    MailSender mailSender;

    ParcelService parcelService;

    @BeforeEach
    void setUp() {
        parcelService = new ParcelService();
        parcelService.parcelRepository = parcelRepository;
        parcelService.historyRepository = historyRepository;
        parcelService.driverEarningsRepository = driverEarningsRepository;
        parcelService.userRepository = userRepository;
        parcelService.pricingService = pricingService();
        parcelService.mailSender = mailSender;
        parcelService.earningsPerDelivery = new BigDecimal("150");
        parcelService.frontendBaseUrl = "http://localhost:3000";
    }

    private PricingService pricingService() {
        PricingService service = new PricingService();
        service.baseCostKes = new BigDecimal("200");
        service.costPerKgKes = new BigDecimal("50");
        return service;
    }

    private BookParcelRequest bookRequest() {
        return new BookParcelRequest("Bob", "254700111222", "bob@example.com", "Moi Ave",
                ParcelCity.NAIROBI, new BigDecimal("2"), null, null, null, "PACKAGE", null);
    }

    @Test
    void bookComputesCostAndSendsConfirmation() {
        UUID senderId = UUID.randomUUID();
        UserEntity sender = new UserEntity();
        sender.email = "jane@example.com";
        sender.name = "Jane";
        when(parcelRepository.existsByTrackingId(anyString())).thenReturn(false);
        when(userRepository.findById(senderId)).thenReturn(sender);

        ParcelDto result = parcelService.book(senderId, bookRequest());

        assertThat(result.costKes()).isEqualByComparingTo("300.00");
        assertThat(result.trackingId()).startsWith("LGS-");
        verify(parcelRepository).persist(any(ParcelEntity.class));
        verify(mailSender).sendBookingConfirmation(eq("jane@example.com"), eq("Jane"), anyString(),
                eq(new BigDecimal("300.00")), anyString(), anyString());
    }

    @Test
    void getByTrackingIdThrowsWhenMissing() {
        when(parcelRepository.findByTrackingId("LGS-MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parcelService.getByTrackingId("LGS-MISSING"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void getByTrackingIdForSenderRejectsNonOwner() {
        ParcelEntity parcel = new ParcelEntity();
        parcel.senderId = UUID.randomUUID();
        when(parcelRepository.findByTrackingId("LGS-ABC123")).thenReturn(Optional.of(parcel));

        assertThatThrownBy(() -> parcelService.getByTrackingIdForSender("LGS-ABC123", UUID.randomUUID()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void getByTrackingIdForSenderReturnsOwnParcel() {
        UUID senderId = UUID.randomUUID();
        ParcelEntity parcel = new ParcelEntity();
        parcel.senderId = senderId;
        parcel.trackingId = "LGS-ABC123";
        when(parcelRepository.findByTrackingId("LGS-ABC123")).thenReturn(Optional.of(parcel));

        ParcelDto result = parcelService.getByTrackingIdForSender("LGS-ABC123", senderId);

        assertThat(result.trackingId()).isEqualTo("LGS-ABC123");
    }

    @Test
    void updateStatusRejectsDriverNotAssignedToParcel() {
        UUID parcelId = UUID.randomUUID();
        UUID otherDriverId = UUID.randomUUID();
        UUID actingDriverId = UUID.randomUUID();

        ParcelEntity parcel = new ParcelEntity();
        parcel.id = parcelId;
        parcel.driverId = otherDriverId;

        UserEntity actingDriver = new UserEntity();
        actingDriver.role = UserRole.DRIVER;

        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));
        when(userRepository.findById(actingDriverId)).thenReturn(actingDriver);

        assertThatThrownBy(() -> parcelService.updateStatus(parcelId, actingDriverId,
                new UpdateStatusRequest(ParcelStatus.DELIVERED, "done")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void updateStatusAllowsAssignedDriver() {
        UUID parcelId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        ParcelEntity parcel = new ParcelEntity();
        parcel.id = parcelId;
        parcel.driverId = driverId;
        parcel.senderId = UUID.randomUUID();
        parcel.trackingId = "LGS-ABC123";
        parcel.costKes = new BigDecimal("300.00");

        UserEntity driver = new UserEntity();
        driver.role = UserRole.DRIVER;

        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));
        when(userRepository.findById(driverId)).thenReturn(driver);
        when(userRepository.findById(parcel.senderId)).thenReturn(null);

        ParcelDto result = parcelService.updateStatus(parcelId, driverId,
                new UpdateStatusRequest(ParcelStatus.IN_TRANSIT, "left warehouse"));

        assertThat(result.status()).isEqualTo(ParcelStatus.IN_TRANSIT);
        verify(historyRepository).persist(any(com.logisights.parcel.entity.ParcelStatusHistoryEntity.class));
    }

    @Test
    void updateStatusToDeliveredAwardsDriverEarningsOnce() {
        UUID parcelId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        ParcelEntity parcel = new ParcelEntity();
        parcel.id = parcelId;
        parcel.driverId = driverId;
        parcel.senderId = UUID.randomUUID();
        parcel.trackingId = "LGS-ABC123";
        parcel.costKes = new BigDecimal("300.00");

        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));
        when(userRepository.findById(parcel.senderId)).thenReturn(null);
        when(driverEarningsRepository.count("parcelId", parcel.id)).thenReturn(0L);

        parcelService.updateStatus(parcelId, null, new UpdateStatusRequest(ParcelStatus.DELIVERED, "handed over"));

        verify(driverEarningsRepository).persist(any(com.logisights.driver.entity.DriverEarningsEntity.class));
    }

    @Test
    void assignDriverThrowsWhenDriverMissing() {
        UUID parcelId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        ParcelEntity parcel = new ParcelEntity();
        parcel.id = parcelId;

        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));
        when(userRepository.findByIdOptional(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parcelService.assignDriver(parcelId, driverId))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void assignDriverSendsNotification() {
        UUID parcelId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        ParcelEntity parcel = new ParcelEntity();
        parcel.id = parcelId;
        parcel.trackingId = "LGS-ABC123";
        parcel.destinationAddress = "Moi Ave";

        UserEntity driver = new UserEntity();
        driver.email = "dan@example.com";
        driver.name = "Dan";

        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));
        when(userRepository.findByIdOptional(driverId)).thenReturn(Optional.of(driver));

        ParcelDto result = parcelService.assignDriver(parcelId, driverId);

        assertThat(result.driverId()).isEqualTo(driverId);
        verify(mailSender).sendDriverAssigned("dan@example.com", "Dan", "LGS-ABC123", "Moi Ave");
    }
}
