package com.mediflow.payment.gateway;

public class PaymentGatewayException
    extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}