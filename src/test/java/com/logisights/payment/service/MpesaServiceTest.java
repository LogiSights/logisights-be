package com.logisights.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logisights.auth.entity.UserEntity;
import com.logisights.auth.repository.UserRepository;
import com.logisights.common.ApiException;
import com.logisights.common.PaymentStatus;
import com.logisights.notification.MailSender;
import com.logisights.parcel.entity.ParcelEntity;
import com.logisights.parcel.repository.ParcelRepository;
import com.logisights.parcel.service.ParcelService;
import com.logisights.payment.client.DarajaApi;
import com.logisights.payment.dto.DarajaStkPushResponse;
import com.logisights.payment.dto.DarajaTokenResponse;
import com.logisights.payment.dto.MpesaCallbackPayload;
import com.logisights.payment.dto.PaymentDto;
import com.logisights.payment.dto.StkPushInitiateRequest;
import com.logisights.payment.entity.PaymentEntity;
import com.logisights.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MpesaServiceTest {

    @Mock
    DarajaApi darajaApi;
    @Mock
    PaymentRepository paymentRepository;
    @Mock
    ParcelRepository parcelRepository;
    @Mock
    ParcelService parcelService;
    @Mock
    UserRepository userRepository;
    @Mock
    MailSender mailSender;

    MpesaService mpesaService;

    @BeforeEach
    void setUp() {
        mpesaService = new MpesaService();
        mpesaService.darajaApi = darajaApi;
        mpesaService.paymentRepository = paymentRepository;
        mpesaService.parcelRepository = parcelRepository;
        mpesaService.parcelService = parcelService;
        mpesaService.userRepository = userRepository;
        mpesaService.mailSender = mailSender;
        mpesaService.objectMapper = new ObjectMapper();
        mpesaService.consumerKey = "key";
        mpesaService.consumerSecret = "secret";
        mpesaService.shortcode = "174379";
        mpesaService.passkey = "passkey";
        mpesaService.callbackUrl = "https://example.com/callback";
    }

    @Test
    void initiateStkPushRejectsNonOwner() {
        UUID parcelId = UUID.randomUUID();
        ParcelEntity parcel = new ParcelEntity();
        parcel.id = parcelId;
        parcel.senderId = UUID.randomUUID();
        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));

        StkPushInitiateRequest request = new StkPushInitiateRequest(parcelId, "254712345678");

        assertThatThrownBy(() -> mpesaService.initiateStkPush(UUID.randomUUID(), request))
                .isInstanceOf(ApiException.class);

        verifyNoInteractions(darajaApi);
    }

    @Test
    void initiateStkPushCreatesPendingPaymentForOwner() {
        UUID parcelId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        ParcelEntity parcel = new ParcelEntity();
        parcel.id = parcelId;
        parcel.senderId = senderId;
        parcel.trackingId = "LGS-ABC123";
        parcel.costKes = new BigDecimal("300.00");
        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));
        when(darajaApi.getAccessToken(anyString(), anyString()))
                .thenReturn(new DarajaTokenResponse("access-token", "3600"));
        when(darajaApi.stkPush(anyString(), any()))
                .thenReturn(new DarajaStkPushResponse("merchant-1", "checkout-1", "0", "Success", "message"));

        PaymentDto result = mpesaService.initiateStkPush(senderId, new StkPushInitiateRequest(parcelId, "254712345678"));

        assertThat(result.providerReference()).isEqualTo("checkout-1");
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository).persist(any(PaymentEntity.class));
    }

    @Test
    void callbackIgnoresUnknownReference() {
        MpesaCallbackPayload payload = callbackPayload("unknown-ref", 0, "300");
        when(paymentRepository.findByProviderReference("unknown-ref")).thenReturn(Optional.empty());

        mpesaService.handleCallback(payload);

        verifyNoInteractions(parcelService, mailSender);
    }

    @Test
    void callbackWithMismatchedAmountMarksFailed() {
        PaymentEntity payment = new PaymentEntity();
        payment.id = UUID.randomUUID();
        payment.parcelId = UUID.randomUUID();
        payment.amountKes = new BigDecimal("300.00");
        when(paymentRepository.findByProviderReference("checkout-1")).thenReturn(Optional.of(payment));

        MpesaCallbackPayload payload = callbackPayload("checkout-1", 0, "50");

        mpesaService.handleCallback(payload);

        assertThat(payment.status).isEqualTo(PaymentStatus.FAILED);
        verify(parcelService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void callbackWithMatchingSuccessMarksPaidAndUpdatesParcel() {
        UUID parcelId = UUID.randomUUID();
        PaymentEntity payment = new PaymentEntity();
        payment.id = UUID.randomUUID();
        payment.parcelId = parcelId;
        payment.amountKes = new BigDecimal("300.00");
        when(paymentRepository.findByProviderReference("checkout-1")).thenReturn(Optional.of(payment));

        ParcelEntity parcel = new ParcelEntity();
        parcel.id = parcelId;
        parcel.senderId = UUID.randomUUID();
        parcel.trackingId = "LGS-ABC123";
        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));

        UserEntity sender = new UserEntity();
        sender.email = "jane@example.com";
        sender.name = "Jane";
        when(userRepository.findById(parcel.senderId)).thenReturn(sender);

        MpesaCallbackPayload payload = callbackPayload("checkout-1", 0, "300");

        mpesaService.handleCallback(payload);

        assertThat(payment.status).isEqualTo(PaymentStatus.SUCCESS);
        verify(parcelService).updateStatus(eq(parcelId), eq(null), any());
        verify(mailSender).sendPaymentConfirmation("jane@example.com", "Jane", "LGS-ABC123", payment.amountKes);
    }

    @Test
    void callbackWithNonZeroResultCodeMarksFailed() {
        UUID parcelId = UUID.randomUUID();
        PaymentEntity payment = new PaymentEntity();
        payment.id = UUID.randomUUID();
        payment.parcelId = parcelId;
        payment.amountKes = new BigDecimal("300.00");
        when(paymentRepository.findByProviderReference("checkout-1")).thenReturn(Optional.of(payment));

        ParcelEntity parcel = new ParcelEntity();
        parcel.id = parcelId;
        parcel.senderId = UUID.randomUUID();
        parcel.trackingId = "LGS-ABC123";
        when(parcelRepository.findByIdOptional(parcelId)).thenReturn(Optional.of(parcel));

        UserEntity sender = new UserEntity();
        sender.email = "jane@example.com";
        sender.name = "Jane";
        when(userRepository.findById(parcel.senderId)).thenReturn(sender);

        MpesaCallbackPayload payload = callbackPayload("checkout-1", 1, "300");

        mpesaService.handleCallback(payload);

        assertThat(payment.status).isEqualTo(PaymentStatus.FAILED);
        verify(mailSender).sendPaymentFailed("jane@example.com", "Jane", "LGS-ABC123");
    }

    private MpesaCallbackPayload callbackPayload(String checkoutRequestId, int resultCode, String amount) {
        var item = new MpesaCallbackPayload.Item("Amount", amount);
        var metadata = new MpesaCallbackPayload.CallbackMetadata(List.of(item));
        var stkCallback = new MpesaCallbackPayload.StkCallback("merchant-1", checkoutRequestId, resultCode,
                "desc", metadata);
        return new MpesaCallbackPayload(new MpesaCallbackPayload.Body(stkCallback));
    }
}
