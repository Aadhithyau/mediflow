package com.mediflow.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mediflow.appointment.AppointmentCompletedEvent;
import com.mediflow.payment.gateway.PaymentGateway;
import com.mediflow.payment.gateway.PaymentGatewayException;
import com.mediflow.payment.gateway.PaymentLinkResult;

@Component
public class AppointmentPaymentListener {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            AppointmentPaymentListener.class
        );

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;

    public AppointmentPaymentListener(
        PaymentService paymentService,
        PaymentGateway paymentGateway
    ) {
        this.paymentService = paymentService;
        this.paymentGateway = paymentGateway;
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleAppointmentCompleted(
        AppointmentCompletedEvent event
    ) {
        PaymentCreationPlan plan;

        try {
            plan = paymentService.preparePayment(
                event.appointmentId()
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Could not prepare payment for appointment {}",
                event.appointmentId(),
                exception
            );
            return;
        }

        if (!plan.requiresGatewayCall()) {
            return;
        }

        PaymentLinkResult result;

        try {
            result = paymentGateway.createPaymentLink(
                plan.request()
            );
        } catch (PaymentGatewayException exception) {
            recordFailure(
                plan.paymentId(),
                exception.getMessage(),
                event.appointmentId(),
                exception
            );
            return;
        } catch (RuntimeException exception) {
            recordFailure(
                plan.paymentId(),
                "Unexpected payment-link creation failure",
                event.appointmentId(),
                exception
            );
            return;
        }

        try {
            paymentService.markPaymentLinkCreated(
                plan.paymentId(),
                result
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Razorpay created a payment link, but "
                    + "MediFlow could not store the result "
                    + "for appointment {} and payment {}",
                event.appointmentId(),
                plan.paymentId(),
                exception
            );
        }
    }

    private void recordFailure(
        Long paymentId,
        String failureMessage,
        Long appointmentId,
        RuntimeException originalException
    ) {
        try {
            paymentService.markPaymentLinkFailed(
                paymentId,
                failureMessage
            );
        } catch (RuntimeException persistenceException) {
            LOGGER.error(
                "Could not record payment-link failure for "
                    + "appointment {} and payment {}",
                appointmentId,
                paymentId,
                persistenceException
            );
        }

        LOGGER.warn(
            "Payment-link creation failed for appointment {}",
            appointmentId,
            originalException
        );
    }
}