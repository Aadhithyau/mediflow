package com.mediflow.payment;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mediflow.appointment.AppointmentCompletedEvent;
import com.mediflow.payment.gateway.PaymentGateway;
import com.mediflow.payment.gateway.PaymentGatewayException;
import com.mediflow.payment.gateway.PaymentLinkRequest;
import com.mediflow.payment.gateway.PaymentLinkResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;


@ExtendWith(MockitoExtension.class)
class AppointmentPaymentListenerTest {

@Mock
private PaymentService paymentService;

@Mock
private PaymentGateway paymentGateway;

@InjectMocks
private AppointmentPaymentListener listener;

@Test
void completedAppointmentCreatesAndStoresPaymentLink() {
    AppointmentCompletedEvent event =
        new AppointmentCompletedEvent(31L);

    PaymentLinkRequest request =
        new PaymentLinkRequest(
            75000L,
            "INR",
            "mediflow-appt-31",
            "MediFlow consultation payment",
            "Listener Test Patient",
            "listener-patient@example.com"
        );

    PaymentCreationPlan plan =
        new PaymentCreationPlan(71L, request);

    PaymentLinkResult result =
        new PaymentLinkResult(
            "plink_test_71",
            "https://rzp.io/i/test71"
        );

    when(paymentService.preparePayment(31L))
        .thenReturn(plan);

    when(paymentGateway.createPaymentLink(request))
        .thenReturn(result);

    listener.handleAppointmentCompleted(event);

    verify(paymentGateway)
        .createPaymentLink(request);

    verify(paymentService)
        .markPaymentLinkCreated(71L, result);

    verify(paymentService, never())
        .markPaymentLinkFailed(
            71L,
            "Payment-link creation failed"
        );
}

@Test
void zeroFeeAppointmentDoesNotCallGateway() {
    AppointmentCompletedEvent event =
        new AppointmentCompletedEvent(32L);

    when(paymentService.preparePayment(32L))
        .thenReturn(
            PaymentCreationPlan.noAction()
        );

    listener.handleAppointmentCompleted(event);

    verify(paymentService)
        .preparePayment(32L);

    verifyNoInteractions(paymentGateway);
}

@Test
void gatewayFailureIsStoredWithoutReopeningAppointment() {
    AppointmentCompletedEvent event =
        new AppointmentCompletedEvent(33L);

    PaymentLinkRequest request =
        new PaymentLinkRequest(
            50000L,
            "INR",
            "mediflow-appt-33",
            "MediFlow consultation payment",
            "Failure Test Patient",
            "failure-patient@example.com"
        );

    PaymentCreationPlan plan =
        new PaymentCreationPlan(73L, request);

    when(paymentService.preparePayment(33L))
        .thenReturn(plan);

    when(paymentGateway.createPaymentLink(request))
        .thenThrow(
            new PaymentGatewayException(
                "Razorpay API credentials "
                    + "are not configured"
            )
        );

    listener.handleAppointmentCompleted(event);

    verify(paymentService)
        .markPaymentLinkFailed(
            73L,
            "Razorpay API credentials "
                + "are not configured"
        );

    verify(paymentService, never())
        .markPaymentLinkCreated(
            73L,
            new PaymentLinkResult(
                "unused",
                "unused"
            )
        );
}


}
