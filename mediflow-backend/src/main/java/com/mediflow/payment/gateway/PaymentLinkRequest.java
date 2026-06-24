package com.mediflow.payment.gateway;

import java.util.Objects;

public record PaymentLinkRequest(
    long amountInSmallestUnit,
    String currency,
    String referenceId,
    String description,
    String customerName,
    String customerEmail
) {

    public PaymentLinkRequest {
        if (amountInSmallestUnit <= 0) {
            throw new IllegalArgumentException(
                "Payment-link amount must be positive"
            );
        }

        Objects.requireNonNull(
            currency,
            "currency is required"
        );

        Objects.requireNonNull(
            referenceId,
            "referenceId is required"
        );

        Objects.requireNonNull(
            description,
            "description is required"
        );

        Objects.requireNonNull(
            customerName,
            "customerName is required"
        );

        Objects.requireNonNull(
            customerEmail,
            "customerEmail is required"
        );
    }
}