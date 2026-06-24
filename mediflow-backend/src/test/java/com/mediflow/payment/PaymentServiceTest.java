package com.mediflow.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mediflow.appointment.Appointment;
import com.mediflow.appointment.AppointmentRepository;
import com.mediflow.appointment.AppointmentStatus;
import com.mediflow.payment.gateway.PaymentLinkResult;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

@Mock
private PaymentRepository paymentRepository;

@Mock
private AppointmentRepository appointmentRepository;

@Mock
private UserRepository userRepository;

@InjectMocks
private PaymentService paymentService;

@Test
void completedPaidAppointmentCreatesPaymentLinkPlan() {
    Long appointmentId = 21L;

    Appointment appointment = mock(Appointment.class);
    User patient = mock(User.class);
    Payment savedPayment = mock(Payment.class);

    when(appointmentRepository.findById(appointmentId))
        .thenReturn(Optional.of(appointment));

    when(appointment.getStatus())
        .thenReturn(AppointmentStatus.COMPLETED);

    when(
        paymentRepository.findByAppointment_Id(
            appointmentId
        )
    )
        .thenReturn(Optional.empty());

    when(appointment.getConsultationFeeSnapshot())
        .thenReturn(new BigDecimal("650.00"));

    when(appointment.getPatient())
        .thenReturn(patient);

    when(patient.getFullName())
        .thenReturn("Payment Test Patient");

    when(patient.getEmail())
        .thenReturn("payment-patient@example.com");

    when(
        paymentRepository.saveAndFlush(
            any(Payment.class)
        )
    )
        .thenReturn(savedPayment);

    when(savedPayment.getId())
        .thenReturn(51L);

    PaymentCreationPlan plan =
        paymentService.preparePayment(appointmentId);

    assertThat(plan.requiresGatewayCall()).isTrue();
    assertThat(plan.paymentId()).isEqualTo(51L);

    assertThat(plan.request().amountInSmallestUnit())
        .isEqualTo(65000L);

    assertThat(plan.request().currency())
        .isEqualTo("INR");

    assertThat(plan.request().referenceId())
        .isEqualTo("mediflow-appt-21");

    assertThat(plan.request().customerName())
        .isEqualTo("Payment Test Patient");

    assertThat(plan.request().customerEmail())
        .isEqualTo("payment-patient@example.com");

    ArgumentCaptor<Payment> paymentCaptor =
        ArgumentCaptor.forClass(Payment.class);

    verify(paymentRepository)
        .saveAndFlush(paymentCaptor.capture());

    Payment createdPayment = paymentCaptor.getValue();

    assertThat(createdPayment.getAppointment())
        .isSameAs(appointment);

    assertThat(createdPayment.getStatus())
        .isEqualTo(
            PaymentStatus.LINK_CREATION_PENDING
        );

    assertThat(createdPayment.getAmount())
        .isEqualByComparingTo("650.00");

    assertThat(createdPayment.getCurrency())
        .isEqualTo("INR");

    assertThat(createdPayment.getRazorpayReferenceId())
        .isEqualTo("mediflow-appt-21");
}

@Test
void zeroFeeAppointmentCreatesNotRequiredPayment() {
    Long appointmentId = 22L;

    Appointment appointment = mock(Appointment.class);

    when(appointmentRepository.findById(appointmentId))
        .thenReturn(Optional.of(appointment));

    when(appointment.getStatus())
        .thenReturn(AppointmentStatus.COMPLETED);

    when(
        paymentRepository.findByAppointment_Id(
            appointmentId
        )
    )
        .thenReturn(Optional.empty());

    when(appointment.getConsultationFeeSnapshot())
        .thenReturn(new BigDecimal("0.00"));

    when(
        paymentRepository.saveAndFlush(
            any(Payment.class)
        )
    )
        .thenAnswer(invocation ->
            invocation.getArgument(0)
        );

    PaymentCreationPlan plan =
        paymentService.preparePayment(appointmentId);

    assertThat(plan.requiresGatewayCall()).isFalse();

    ArgumentCaptor<Payment> paymentCaptor =
        ArgumentCaptor.forClass(Payment.class);

    verify(paymentRepository)
        .saveAndFlush(paymentCaptor.capture());

    Payment createdPayment = paymentCaptor.getValue();

    assertThat(createdPayment.getStatus())
        .isEqualTo(PaymentStatus.NOT_REQUIRED);

    assertThat(createdPayment.getAmount())
        .isEqualByComparingTo("0.00");

    assertThat(createdPayment.getRazorpayReferenceId())
        .isNull();
}

@Test
void existingAppointmentPaymentIsNotCreatedAgain() {
    Long appointmentId = 23L;

    Appointment appointment = mock(Appointment.class);
    Payment existingPayment = mock(Payment.class);

    when(appointmentRepository.findById(appointmentId))
        .thenReturn(Optional.of(appointment));

    when(appointment.getStatus())
        .thenReturn(AppointmentStatus.COMPLETED);

    when(
        paymentRepository.findByAppointment_Id(
            appointmentId
        )
    )
        .thenReturn(Optional.of(existingPayment));

    PaymentCreationPlan plan =
        paymentService.preparePayment(appointmentId);

    assertThat(plan.requiresGatewayCall()).isFalse();

    verify(paymentRepository, never())
        .saveAndFlush(any(Payment.class));
}

@Test
void bookedAppointmentCannotPreparePayment() {
    Long appointmentId = 24L;

    Appointment appointment = mock(Appointment.class);

    when(appointmentRepository.findById(appointmentId))
        .thenReturn(Optional.of(appointment));

    when(appointment.getStatus())
        .thenReturn(AppointmentStatus.BOOKED);

    assertThatThrownBy(
        () -> paymentService.preparePayment(
            appointmentId
        )
    )
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Payment can only be prepared for a "
                + "completed appointment"
        );

    verify(paymentRepository, never())
        .saveAndFlush(any(Payment.class));
}

@Test
void createdPaymentLinkChangesStatusToPending() {
    Payment payment = new Payment();

    payment.setStatus(
        PaymentStatus.LINK_CREATION_PENDING
    );

    when(paymentRepository.findById(61L))
        .thenReturn(Optional.of(payment));

    PaymentLinkResult result = new PaymentLinkResult(
        "plink_test_61",
        "https://rzp.io/i/test61"
    );

    paymentService.markPaymentLinkCreated(
        61L,
        result
    );

    assertThat(payment.getStatus())
        .isEqualTo(PaymentStatus.PAYMENT_PENDING);

    assertThat(payment.getRazorpayPaymentLinkId())
        .isEqualTo("plink_test_61");

    assertThat(payment.getRazorpayPaymentLinkUrl())
        .isEqualTo("https://rzp.io/i/test61");

    assertThat(payment.getFailureMessage()).isNull();

    verify(paymentRepository)
        .saveAndFlush(payment);
}

@Test
void failedPaymentLinkChangesStatusToFailed() {
    Payment payment = new Payment();

    payment.setStatus(
        PaymentStatus.LINK_CREATION_PENDING
    );

    when(paymentRepository.findById(62L))
        .thenReturn(Optional.of(payment));

    paymentService.markPaymentLinkFailed(
        62L,
        "Razorpay is unavailable"
    );

    assertThat(payment.getStatus())
        .isEqualTo(
            PaymentStatus.LINK_CREATION_FAILED
        );

    assertThat(payment.getFailureMessage())
        .isEqualTo("Razorpay is unavailable");

    verify(paymentRepository)
        .saveAndFlush(payment);
}


}
