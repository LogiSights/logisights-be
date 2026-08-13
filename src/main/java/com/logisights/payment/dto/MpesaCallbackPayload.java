package com.logisights.payment.dto;

import java.util.List;

public record MpesaCallbackPayload(Body Body) {

    public record Body(StkCallback stkCallback) {
    }

    public record StkCallback(
            String MerchantRequestID,
            String CheckoutRequestID,
            int ResultCode,
            String ResultDesc,
            CallbackMetadata CallbackMetadata
    ) {
    }

    public record CallbackMetadata(List<Item> Item) {
    }

    public record Item(String Name, Object Value) {
    }
}
