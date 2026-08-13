package com.logisights.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logisights.auth.entity.UserEntity;
import com.logisights.auth.repository.UserRepository;
import com.logisights.common.ApiException;
import com.logisights.common.ParcelStatus;
import com.logisights.common.PaymentStatus;
import com.logisights.notification.MailSender;
import com.logisights.parcel.dto.UpdateStatusRequest;
import com.logisights.parcel.entity.ParcelEntity;
import com.logisights.parcel.repository.ParcelRepository;
import com.logisights.parcel.service.ParcelService;
import com.logisights.payment.client.DarajaApi;
import com.logisights.payment.dto.*;
import com.logisights.payment.entity.PaymentEntity;
import com.logisights.payment.repository.PaymentRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@ApplicationScoped
public class MpesaService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @RestClient
    DarajaApi darajaApi;

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    ParcelRepository parcelRepository;

    @Inject
    ParcelService parcelService;

    @Inject
    UserRepository userRepository;

    @Inject
    MailSender mailSender;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "mpesa.consumer-key")
    String consumerKey;

    @ConfigProperty(name = "mpesa.consumer-secret")
    String consumerSecret;

    @ConfigProperty(name = "mpesa.shortcode")
    String shortcode;

    @ConfigProperty(name = "mpesa.passkey")
    String passkey;

    @ConfigProperty(name = "mpesa.callback-url")
    String callbackUrl;

    @Transactional
    public PaymentDto initiateStkPush(java.util.UUID senderId, StkPushInitiateRequest request) {
        ParcelEntity parcel = parcelRepository.findByIdOptional(request.parcelId())
                .orElseThrow(() -> ApiException.notFound("Parcel not found"));

        if (!parcel.senderId.equals(senderId)) {
            throw ApiException.forbidden("You do not own this parcel");
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String password = Base64.getEncoder().encodeToString(
                (shortcode + passkey + timestamp).getBytes(StandardCharsets.UTF_8));

        String accessToken = fetchAccessToken();

        var darajaRequest = new DarajaStkPushRequest(
                shortcode,
                password,
                timestamp,
                "CustomerPayBillOnline",
                parcel.costKes.toBigInteger().toString(),
                request.phone(),
                shortcode,
                request.phone(),
                callbackUrl,
                parcel.trackingId,
                "Logisights delivery payment"
        );

        DarajaStkPushResponse response = darajaApi.stkPush("Bearer " + accessToken, darajaRequest);

        PaymentEntity payment = new PaymentEntity();
        payment.parcelId = parcel.id;
        payment.providerReference = response.CheckoutRequestID();
        payment.phone = request.phone();
        payment.amountKes = parcel.costKes;
        payment.status = PaymentStatus.PENDING;
        paymentRepository.persist(payment);

        return PaymentDto.from(payment);
    }

    @Transactional
    public void handleCallback(MpesaCallbackPayload payload) {
        var stkCallback = payload.Body().stkCallback();
        String reference = stkCallback.CheckoutRequestID();

        PaymentEntity payment = paymentRepository.findByProviderReference(reference)
                .orElse(null);
        if (payment == null) {
            Log.warn("M-Pesa callback for unknown CheckoutRequestID: " + reference);
            return;
        }

        try {
            payment.rawCallback = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            Log.error("Failed to serialize M-Pesa callback payload", e);
        }

        boolean success = stkCallback.ResultCode() == 0 && callbackAmountMatches(stkCallback, payment);
        payment.status = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;

        ParcelEntity parcel = parcelRepository.findByIdOptional(payment.parcelId).orElse(null);
        UserEntity sender = parcel != null ? userRepository.findById(parcel.senderId) : null;

        if (success) {
            parcelService.updateStatus(payment.parcelId, null,
                    new UpdateStatusRequest(ParcelStatus.IN_TRANSIT, "Payment confirmed via M-Pesa"));
            if (sender != null && parcel != null) {
                mailSender.sendPaymentConfirmation(sender.email, sender.name, parcel.trackingId, payment.amountKes);
            }
        } else if (sender != null && parcel != null) {
            mailSender.sendPaymentFailed(sender.email, sender.name, parcel.trackingId);
        }
    }

    private boolean callbackAmountMatches(MpesaCallbackPayload.StkCallback stkCallback, PaymentEntity payment) {
        if (stkCallback.CallbackMetadata() == null) {
            return false;
        }
        return stkCallback.CallbackMetadata().Item().stream()
                .filter(item -> "Amount".equals(item.Name()))
                .findFirst()
                .map(item -> {
                    java.math.BigDecimal callbackAmount = new java.math.BigDecimal(item.Value().toString());
                    boolean matches = callbackAmount.compareTo(payment.amountKes) == 0;
                    if (!matches) {
                        Log.warn("M-Pesa callback amount " + callbackAmount + " does not match expected "
                                + payment.amountKes + " for payment " + payment.id);
                    }
                    return matches;
                })
                .orElse(false);
    }

    private String fetchAccessToken() {
        String credentials = Base64.getEncoder().encodeToString(
                (consumerKey + ":" + consumerSecret).getBytes(StandardCharsets.UTF_8));
        DarajaTokenResponse response = darajaApi.getAccessToken("client_credentials", "Basic " + credentials);
        return response.accessToken();
    }
}
