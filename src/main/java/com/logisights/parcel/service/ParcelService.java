package com.logisights.parcel.service;

import com.logisights.common.ApiException;
import com.logisights.common.ParcelStatus;
import com.logisights.driver.entity.DriverEarningsEntity;
import com.logisights.driver.repository.DriverEarningsRepository;
import com.logisights.parcel.dto.BookParcelRequest;
import com.logisights.parcel.dto.ParcelDto;
import com.logisights.parcel.dto.UpdateStatusRequest;
import com.logisights.parcel.entity.ParcelEntity;
import com.logisights.parcel.entity.ParcelStatusHistoryEntity;
import com.logisights.parcel.repository.ParcelRepository;
import com.logisights.parcel.repository.ParcelStatusHistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ParcelService {

    private static final String TRACKING_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    @Inject
    ParcelRepository parcelRepository;

    @Inject
    ParcelStatusHistoryRepository historyRepository;

    @Inject
    DriverEarningsRepository driverEarningsRepository;

    @Inject
    PricingService pricingService;

    @ConfigProperty(name = "driver.earnings-per-delivery-kes")
    BigDecimal earningsPerDelivery;

    @Transactional
    public ParcelDto book(UUID senderId, BookParcelRequest request) {
        ParcelEntity parcel = new ParcelEntity();
        parcel.trackingId = generateTrackingId();
        parcel.senderId = senderId;
        parcel.recipientName = request.recipientName();
        parcel.recipientPhone = request.recipientPhone();
        parcel.destinationAddress = request.destinationAddress();
        parcel.city = request.city();
        parcel.weightKg = request.weightKg();
        parcel.lengthCm = request.lengthCm();
        parcel.widthCm = request.widthCm();
        parcel.heightCm = request.heightCm();
        parcel.parcelType = request.parcelType();
        parcel.pickupPointId = request.pickupPointId();
        parcel.costKes = pricingService.computeCost(request.weightKg());
        parcel.status = ParcelStatus.PENDING;
        parcelRepository.persist(parcel);

        recordHistory(parcel.id, ParcelStatus.PENDING, senderId, "Parcel booked");

        return ParcelDto.from(parcel);
    }

    public ParcelDto getByTrackingId(String trackingId) {
        return parcelRepository.findByTrackingId(trackingId)
                .map(ParcelDto::from)
                .orElseThrow(() -> ApiException.notFound("No parcel found for tracking ID " + trackingId));
    }

    public List<ParcelDto> listForSender(UUID senderId) {
        return parcelRepository.findBySender(senderId).stream().map(ParcelDto::from).collect(Collectors.toList());
    }

    public List<ParcelDto> listForDriver(UUID driverId) {
        return parcelRepository.findByDriver(driverId).stream().map(ParcelDto::from).collect(Collectors.toList());
    }

    @Transactional
    public ParcelDto updateStatus(UUID parcelId, UUID actorId, UpdateStatusRequest request) {
        ParcelEntity parcel = parcelRepository.findByIdOptional(parcelId)
                .orElseThrow(() -> ApiException.notFound("Parcel not found"));

        parcel.status = request.status();
        recordHistory(parcel.id, request.status(), actorId, request.note());

        if (request.status() == ParcelStatus.DELIVERED && parcel.driverId != null) {
            awardDriverEarnings(parcel);
        }

        return ParcelDto.from(parcel);
    }

    private void awardDriverEarnings(ParcelEntity parcel) {
        boolean alreadyPaid = driverEarningsRepository.count("parcelId", parcel.id) > 0;
        if (alreadyPaid) {
            return;
        }
        DriverEarningsEntity earnings = new DriverEarningsEntity();
        earnings.driverId = parcel.driverId;
        earnings.parcelId = parcel.id;
        earnings.amountKes = earningsPerDelivery;
        driverEarningsRepository.persist(earnings);
    }

    private void recordHistory(UUID parcelId, ParcelStatus status, UUID changedBy, String note) {
        ParcelStatusHistoryEntity history = new ParcelStatusHistoryEntity();
        history.parcelId = parcelId;
        history.status = status;
        history.changedBy = changedBy;
        history.note = note;
        historyRepository.persist(history);
    }

    private String generateTrackingId() {
        String candidate;
        do {
            StringBuilder sb = new StringBuilder("LGS-");
            for (int i = 0; i < 6; i++) {
                sb.append(TRACKING_ALPHABET.charAt(random.nextInt(TRACKING_ALPHABET.length())));
            }
            candidate = sb.toString();
        } while (parcelRepository.existsByTrackingId(candidate));
        return candidate;
    }
}
