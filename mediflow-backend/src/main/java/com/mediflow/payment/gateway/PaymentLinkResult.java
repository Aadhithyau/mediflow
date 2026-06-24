package com.mediflow.payment.gateway;

import java.util.Objects;

public record PaymentLinkResult(
    String paymentLinkId,
    String paymentLinkUrl
) {

    public PaymentLinkResult {
        Objects.requireNonNull(
            paymentLinkId,
            "paymentLinkId is required"
        );

        Objects.requireNonNull(
            paymentLinkUrl,
            "paymentLinkUrl is required"
        );
    }
}