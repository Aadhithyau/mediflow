package com.mediflow.payment;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.appointment.Appointment;
import com.mediflow.appointment.AppointmentRepository;
import com.mediflow.appointment.AppointmentStatus;
import com.mediflow.payment.dto.PaymentResponse;
import com.mediflow.payment.gateway.PaymentLinkRequest;
import com.mediflow.payment.gateway.PaymentLinkResult;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@Service
public class PaymentService {

    private static final String DEFAULT_CURRENCY = "INR";

    private static final String REFERENCE_PREFIX =
        "mediflow-appt-";

    private static final int MAX_FAILURE_MESSAGE_LENGTH = 500;

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public PaymentService(
        PaymentRepository paymentRepository,
        AppointmentRepository appointmentRepository,
        UserRepository userRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public PaymentCreationPlan preparePayment(
        Long appointmentId
    ) {
        Appointment appointment = appointmentRepository
            .findById(appointmentId)
            .orElseThrow(() -> new IllegalStateException(
                "Completed appointment was not found"
            ));

        if (
            appointment.getStatus()
                != AppointmentStatus.COMPLETED
        ) {
            throw new IllegalStateException(
                "Payment can only be prepared for a "
                    + "completed appointment"
            );
        }

        if (
            paymentRepository
                .findByAppointment_Id(appointmentId)
                .isPresent()
        ) {
            return PaymentCreationPlan.noAction();
        }

        BigDecimal amount =
            appointment.getConsultationFeeSnapshot();

        if (amount == null || amount.signum() < 0) {
            throw new IllegalStateException(
                "Appointment has an invalid payment amount"
            );
        }

        Payment payment = new Payment();

        payment.setAppointment(appointment);
        payment.setAmount(amount);
        payment.setCurrency(DEFAULT_CURRENCY);

        if (amount.signum() == 0) {
            payment.setStatus(PaymentStatus.NOT_REQUIRED);
            paymentRepository.saveAndFlush(payment);

            return PaymentCreationPlan.noAction();
        }

        String referenceId =
            REFERENCE_PREFIX + appointmentId;

        payment.setStatus(
            PaymentStatus.LINK_CREATION_PENDING
        );
        payment.setRazorpayReferenceId(referenceId);

        Payment savedPayment =
            paymentRepository.saveAndFlush(payment);

        User patient = appointment.getPatient();

        PaymentLinkRequest request =
            new PaymentLinkRequest(
                toSmallestCurrencyUnit(amount),
                DEFAULT_CURRENCY,
                referenceId,
                "MediFlow consultation payment for "
                    + "appointment "
                    + appointmentId,
                patient.getFullName(),
                patient.getEmail()
            );

        return new PaymentCreationPlan(
            savedPayment.getId(),
            request
        );
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void markPaymentLinkCreated(
        Long paymentId,
        PaymentLinkResult result
    ) {
        Payment payment = paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new IllegalStateException(
                "Payment record was not found"
            ));

        if (
            payment.getStatus()
                != PaymentStatus.LINK_CREATION_PENDING
        ) {
            return;
        }

        payment.setRazorpayPaymentLinkId(
            result.paymentLinkId()
        );
        payment.setRazorpayPaymentLinkUrl(
            result.paymentLinkUrl()
        );
        payment.setFailureMessage(null);
        payment.setStatus(PaymentStatus.PAYMENT_PENDING);

        paymentRepository.saveAndFlush(payment);
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void markPaymentLinkFailed(
        Long paymentId,
        String failureMessage
    ) {
        Payment payment = paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new IllegalStateException(
                "Payment record was not found"
            ));

        if (
            payment.getStatus()
                != PaymentStatus.LINK_CREATION_PENDING
        ) {
            return;
        }

        payment.setFailureMessage(
            normaliseFailureMessage(failureMessage)
        );
        payment.setStatus(
            PaymentStatus.LINK_CREATION_FAILED
        );

        paymentRepository.saveAndFlush(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentForPatient(
        String patientEmail,
        Long appointmentId
    ) {
        User patient = findEnabledPatient(patientEmail);

        Payment payment = paymentRepository
            .findForPatient(
                appointmentId,
                patient.getId()
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment was not found for this appointment"
            ));

        return toResponse(payment);
    }

    private User findEnabledPatient(String patientEmail) {
        return userRepository.findByEmail(patientEmail)
            .filter(User::isEnabled)
            .filter(user -> user.getRole() == Role.PATIENT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Patient account is unavailable"
            ));
    }

    private long toSmallestCurrencyUnit(
        BigDecimal amount
    ) {
        try {
            return amount
                .movePointRight(2)
                .longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                "Appointment payment amount cannot be "
                    + "converted to the smallest currency unit",
                exception
            );
        }
    }

    private String normaliseFailureMessage(
        String failureMessage
    ) {
        String normalisedMessage =
            StringUtils.hasText(failureMessage)
                ? failureMessage.strip()
                : "Payment-link creation failed";

        if (
            normalisedMessage.length()
                <= MAX_FAILURE_MESSAGE_LENGTH
        ) {
            return normalisedMessage;
        }

        return normalisedMessage.substring(
            0,
            MAX_FAILURE_MESSAGE_LENGTH
        );
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getAppointment().getId(),
            payment.getStatus(),
            payment.getAmount(),
            payment.getCurrency()
                .toUpperCase(Locale.ROOT),
            payment.getRazorpayPaymentLinkUrl(),
            payment.getRazorpayPaymentId(),
            payment.getPaidAt(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}