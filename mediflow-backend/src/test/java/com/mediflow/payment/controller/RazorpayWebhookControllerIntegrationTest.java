package com.mediflow.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.mediflow.appointment.Appointment;
import com.mediflow.appointment.AppointmentRepository;
import com.mediflow.appointment.AppointmentStatus;
import com.mediflow.availability.DoctorAvailabilitySlot;
import com.mediflow.availability.DoctorAvailabilitySlotRepository;
import com.mediflow.doctor.DoctorProfile;
import com.mediflow.doctor.DoctorProfileRepository;
import com.mediflow.payment.Payment;
import com.mediflow.payment.PaymentRepository;
import com.mediflow.payment.PaymentStatus;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest(
properties = {
"app.razorpay.webhook-secret=test-webhook-secret"
}
)
@AutoConfigureMockMvc
@Transactional
class RazorpayWebhookControllerIntegrationTest {

private static final String WEBHOOK_SECRET =
    "test-webhook-secret";

private static final String WEBHOOK_ENDPOINT =
    "/api/payments/webhooks/razorpay";

@Autowired
private MockMvc mockMvc;

@Autowired
private UserRepository userRepository;

@Autowired
private DoctorProfileRepository doctorProfileRepository;

@Autowired
private DoctorAvailabilitySlotRepository slotRepository;

@Autowired
private AppointmentRepository appointmentRepository;

@Autowired
private PaymentRepository paymentRepository;

@PersistenceContext
private EntityManager entityManager;

@Test
void validWebhookMarksPaymentPaidAndDuplicateIsIgnored()
    throws Exception {

    TestPaymentData data = createPendingPaymentData();

    String eventId =
        "evt_test_" + UUID.randomUUID();

    String razorpayPaymentId =
        "pay_test_" + UUID.randomUUID();

    long paymentCreatedAt =
        Instant.parse("2030-01-01T10:15:30Z")
            .getEpochSecond();

    byte[] rawBody = createPaidWebhookBody(
        data.paymentLinkId(),
        data.referenceId(),
        razorpayPaymentId,
        70000L,
        paymentCreatedAt
    ).getBytes(StandardCharsets.UTF_8);

    String signature = createSignature(rawBody);

    mockMvc.perform(
        post(WEBHOOK_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                "X-Razorpay-Signature",
                signature
            )
            .header(
                "X-Razorpay-Event-Id",
                eventId
            )
            .content(rawBody)
    )
        .andExpect(status().isNoContent());

    entityManager.flush();
    entityManager.clear();

    Payment paidPayment = paymentRepository
        .findById(data.paymentId())
        .orElseThrow();

    assertThat(paidPayment.getStatus())
    .isEqualTo(PaymentStatus.PAID);


    assertThat(paidPayment.getRazorpayPaymentId())
        .isEqualTo(razorpayPaymentId);

    assertThat(paidPayment.getRazorpayWebhookEventId())
        .isEqualTo(eventId);

    assertThat(paidPayment.getPaidAt())
        .isNotNull();

    assertThat(paidPayment.getPaidAt().toInstant())
        .isEqualTo(
            Instant.ofEpochSecond(paymentCreatedAt)
        );

    mockMvc.perform(
        post(WEBHOOK_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                "X-Razorpay-Signature",
                signature
            )
            .header(
                "X-Razorpay-Event-Id",
                eventId
            )
            .content(rawBody)
    )
        .andExpect(status().isNoContent());

    entityManager.flush();
    entityManager.clear();

    Payment paymentAfterDuplicate =
        paymentRepository
            .findById(data.paymentId())
            .orElseThrow();

    assertThat(paymentAfterDuplicate.getStatus())
        .isEqualTo(PaymentStatus.PAID);

    assertThat(
        paymentAfterDuplicate.getRazorpayPaymentId()
    )
        .isEqualTo(razorpayPaymentId);
}

@Test
void invalidWebhookSignatureIsRejected()
    throws Exception {

    TestPaymentData data = createPendingPaymentData();

    byte[] rawBody = createPaidWebhookBody(
        data.paymentLinkId(),
        data.referenceId(),
        "pay_invalid_signature",
        70000L,
        Instant.now().getEpochSecond()
    ).getBytes(StandardCharsets.UTF_8);

    mockMvc.perform(
        post(WEBHOOK_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                "X-Razorpay-Signature",
                "0011223344556677"
            )
            .header(
                "X-Razorpay-Event-Id",
                "evt_invalid_signature"
            )
            .content(rawBody)
    )
        .andExpect(status().isUnauthorized());

    entityManager.flush();
    entityManager.clear();

    Payment unchangedPayment =
        paymentRepository
            .findById(data.paymentId())
            .orElseThrow();

    assertThat(unchangedPayment.getStatus())
        .isEqualTo(PaymentStatus.PAYMENT_PENDING);

    assertThat(unchangedPayment.getRazorpayPaymentId())
        .isNull();

    assertThat(unchangedPayment.getPaidAt())
        .isNull();
}

@Test
void webhookWithIncorrectAmountIsRejected()
    throws Exception {

    TestPaymentData data = createPendingPaymentData();

    byte[] rawBody = createPaidWebhookBody(
        data.paymentLinkId(),
        data.referenceId(),
        "pay_wrong_amount",
        69999L,
        Instant.now().getEpochSecond()
    ).getBytes(StandardCharsets.UTF_8);

    String signature = createSignature(rawBody);

    mockMvc.perform(
        post(WEBHOOK_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                "X-Razorpay-Signature",
                signature
            )
            .header(
                "X-Razorpay-Event-Id",
                "evt_wrong_amount_"
                    + UUID.randomUUID()
            )
            .content(rawBody)
    )
        .andExpect(status().isBadRequest());

    entityManager.flush();
    entityManager.clear();

    Payment unchangedPayment =
        paymentRepository
            .findById(data.paymentId())
            .orElseThrow();

    assertThat(unchangedPayment.getStatus())
        .isEqualTo(PaymentStatus.PAYMENT_PENDING);

    assertThat(unchangedPayment.getRazorpayPaymentId())
        .isNull();
}

private TestPaymentData createPendingPaymentData() {
    String uniqueValue = UUID.randomUUID().toString();

    User doctor = createUser(
        "Webhook Doctor",
        "webhook-doctor-" + uniqueValue + "@example.com",
        Role.DOCTOR
    );

    User patient = createUser(
        "Webhook Patient",
        "webhook-patient-" + uniqueValue + "@example.com",
        Role.PATIENT
    );

    DoctorProfile doctorProfile =
        new DoctorProfile();

    doctorProfile.setUser(doctor);
    doctorProfile.setSpecialization(
        "General Medicine"
    );
    doctorProfile.setMedicalLicenseNumber(
        "WEBHOOK-" + uniqueValue
    );
    doctorProfile.setConsultationFee(
        new BigDecimal("700.00")
    );
    doctorProfile.setBio(
        "Doctor profile created for webhook testing."
    );

    doctorProfile =
        doctorProfileRepository.saveAndFlush(
            doctorProfile
        );

    OffsetDateTime startTime =
        OffsetDateTime.now(ZoneOffset.UTC)
            .withNano(0)
            .plusDays(1);

    DoctorAvailabilitySlot slot =
        new DoctorAvailabilitySlot();

    slot.setDoctorProfile(doctorProfile);
    slot.setStartTime(startTime);
    slot.setEndTime(startTime.plusHours(1));

    slot = slotRepository.saveAndFlush(slot);

    Appointment appointment = new Appointment();

    appointment.setAvailabilitySlot(slot);
    appointment.setPatient(patient);
    appointment.setStatus(AppointmentStatus.COMPLETED);
    appointment.setConsultationFeeSnapshot(
        new BigDecimal("700.00")
    );
    appointment.setCompletedAt(
        OffsetDateTime.now(ZoneOffset.UTC)
            .withNano(0)
    );

    appointment =
        appointmentRepository.saveAndFlush(
            appointment
        );

    String referenceId =
        "mediflow-appt-" + appointment.getId();

    String paymentLinkId =
        "plink_test_" + appointment.getId();

    Payment payment = new Payment();

    payment.setAppointment(appointment);
    payment.setStatus(PaymentStatus.PAYMENT_PENDING);
    payment.setAmount(new BigDecimal("700.00"));
    payment.setCurrency("INR");
    payment.setRazorpayReferenceId(referenceId);
    payment.setRazorpayPaymentLinkId(paymentLinkId);
    payment.setRazorpayPaymentLinkUrl(
        "https://rzp.io/i/webhook-"
            + appointment.getId()
    );

    payment = paymentRepository.saveAndFlush(payment);

    return new TestPaymentData(
        payment.getId(),
        paymentLinkId,
        referenceId
    );
}

private User createUser(
    String fullName,
    String email,
    Role role
) {
    User user = new User();

    user.setFullName(fullName);
    user.setEmail(email);
    user.setPasswordHash(
        "integration-test-password-hash"
    );
    user.setRole(role);
    user.setEnabled(true);

    return userRepository.saveAndFlush(user);
}

private String createPaidWebhookBody(
    String paymentLinkId,
    String referenceId,
    String razorpayPaymentId,
    long amount,
    long createdAt
) {
    return "{"
        + "\"event\":\"payment_link.paid\","
        + "\"payload\":{"
        + "\"payment_link\":{\"entity\":{"
        + "\"id\":\"" + paymentLinkId + "\","
        + "\"reference_id\":\"" + referenceId + "\","
        + "\"status\":\"paid\","
        + "\"currency\":\"INR\","
        + "\"amount\":" + amount + ","
        + "\"amount_paid\":" + amount
        + "}},"
        + "\"payment\":{\"entity\":{"
        + "\"id\":\"" + razorpayPaymentId + "\","
        + "\"status\":\"captured\","
        + "\"currency\":\"INR\","
        + "\"amount\":" + amount + ","
        + "\"captured\":true,"
        + "\"created_at\":" + createdAt
        + "}}"
        + "}"
        + "}";
}

private String createSignature(
    byte[] rawBody
) throws Exception {

    Mac mac = Mac.getInstance("HmacSHA256");

    mac.init(
        new SecretKeySpec(
            WEBHOOK_SECRET.getBytes(
                StandardCharsets.UTF_8
            ),
            "HmacSHA256"
        )
    );

    return HexFormat.of().formatHex(
        mac.doFinal(rawBody)
    );
}

private record TestPaymentData(
    Long paymentId,
    String paymentLinkId,
    String referenceId
) {
}


}
