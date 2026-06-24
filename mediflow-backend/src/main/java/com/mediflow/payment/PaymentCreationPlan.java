package com.mediflow.payment;

import com.mediflow.payment.gateway.PaymentLinkRequest;

public record PaymentCreationPlan(
    Long paymentId,
    PaymentLinkRequest request
) {

    public PaymentCreationPlan {
        boolean paymentIdMissing = paymentId == null;
        boolean requestMissing = request == null;

        if (paymentIdMissing != requestMissing) {
            throw new IllegalArgumentException(
                "Payment ID and request must either both "
                    + "be present or both be absent"
            );
        }
    }

    public static PaymentCreationPlan noAction() {
        return new PaymentCreationPlan(null, null);
    }

    public boolean requiresGatewayCall() {
        return paymentId != null;
    }
}