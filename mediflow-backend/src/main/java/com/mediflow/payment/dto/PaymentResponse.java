package com.mediflow.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.mediflow.payment.PaymentStatus;

public record PaymentResponse(
    Long id,
    Long appointmentId,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String paymentLinkUrl,
    String razorpayPaymentId,
    OffsetDateTime paidAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}