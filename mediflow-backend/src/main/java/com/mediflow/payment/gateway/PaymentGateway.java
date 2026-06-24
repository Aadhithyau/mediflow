package com.mediflow.payment.gateway;

public interface PaymentGateway {

    PaymentLinkResult createPaymentLink(
        PaymentLinkRequest request
    );
}