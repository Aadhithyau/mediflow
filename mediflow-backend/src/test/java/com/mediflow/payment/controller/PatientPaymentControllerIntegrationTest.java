package com.mediflow.payment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.mediflow.appointment.Appointment;
import com.mediflow.appointment.AppointmentRepository;
import com.mediflow.appointment.AppointmentStatus;
import com.mediflow.auth.JwtService;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientPaymentControllerIntegrationTest {

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

@Autowired
private JwtService jwtService;

@Test
void patientReadsOnlyPaymentForOwnedAppointment()
    throws Exception {

    String uniqueValue = UUID.randomUUID().toString();

    User doctor = createUser(
        "Payment Endpoint Doctor",
        "payment-doctor-" + uniqueValue + "@example.com",
        Role.DOCTOR
    );

    User ownerPatient = createUser(
        "Payment Owner Patient",
        "payment-owner-" + uniqueValue + "@example.com",
        Role.PATIENT
    );

    User otherPatient = createUser(
        "Other Payment Patient",
        "payment-other-" + uniqueValue + "@example.com",
        Role.PATIENT
    );

    DoctorProfile doctorProfile = createDoctorProfile(
        doctor,
        uniqueValue
    );

    OffsetDateTime startTime =
        OffsetDateTime.now(ZoneOffset.UTC)
            .withNano(0)
            .plusDays(1);

    DoctorAvailabilitySlot slot = createSlot(
        doctorProfile,
        startTime,
        startTime.plusHours(1)
    );

    Appointment appointment = createCompletedAppointment(
        slot,
        ownerPatient
    );

    Payment payment = createPendingPayment(appointment);

    String endpoint =
        "/api/appointments/"
            + appointment.getId()
            + "/payment";

    String ownerToken =
        jwtService.generateAccessToken(ownerPatient);

    mockMvc.perform(
        get(endpoint)
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + ownerToken
            )
    )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.id")
                .value(payment.getId())
        )
        .andExpect(
            jsonPath("$.appointmentId")
                .value(appointment.getId())
        )
        .andExpect(
            jsonPath("$.status")
                .value("PAYMENT_PENDING")
        )
        .andExpect(
            jsonPath("$.amount")
                .value(700.00)
        )
        .andExpect(
            jsonPath("$.currency")
                .value("INR")
        )
        .andExpect(
            jsonPath("$.paymentLinkUrl")
                .value(
                    payment.getRazorpayPaymentLinkUrl()
                )
        )
        .andExpect(
            jsonPath("$.razorpayPaymentId")
                .doesNotExist()
        )
        .andExpect(
            jsonPath("$.paidAt")
                .doesNotExist()
        );

    String otherPatientToken =
        jwtService.generateAccessToken(otherPatient);

    mockMvc.perform(
        get(endpoint)
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + otherPatientToken
            )
    )
        .andExpect(status().isNotFound());
}

@Test
void doctorAndUnauthenticatedUserCannotAccessPatientPayment()
    throws Exception {

    String uniqueValue = UUID.randomUUID().toString();

    User doctor = createUser(
        "Blocked Payment Doctor",
        "blocked-payment-doctor-"
            + uniqueValue
            + "@example.com",
        Role.DOCTOR
    );

    String doctorToken =
        jwtService.generateAccessToken(doctor);

    mockMvc.perform(
        get("/api/appointments/1/payment")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + doctorToken
            )
    )
        .andExpect(status().isForbidden());

    mockMvc.perform(
        get("/api/appointments/1/payment")
    )
        .andExpect(status().isUnauthorized());
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

private DoctorProfile createDoctorProfile(
    User doctor,
    String uniqueValue
) {
    DoctorProfile doctorProfile = new DoctorProfile();

    doctorProfile.setUser(doctor);
    doctorProfile.setSpecialization(
        "General Medicine"
    );
    doctorProfile.setMedicalLicenseNumber(
        "PAYMENT-" + uniqueValue
    );
    doctorProfile.setConsultationFee(
        new BigDecimal("700.00")
    );
    doctorProfile.setBio(
        "Doctor profile created for payment testing."
    );

    return doctorProfileRepository.saveAndFlush(
        doctorProfile
    );
}

private DoctorAvailabilitySlot createSlot(
    DoctorProfile doctorProfile,
    OffsetDateTime startTime,
    OffsetDateTime endTime
) {
    DoctorAvailabilitySlot slot =
        new DoctorAvailabilitySlot();

    slot.setDoctorProfile(doctorProfile);
    slot.setStartTime(startTime);
    slot.setEndTime(endTime);

    return slotRepository.saveAndFlush(slot);
}

private Appointment createCompletedAppointment(
    DoctorAvailabilitySlot slot,
    User patient
) {
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

    return appointmentRepository.saveAndFlush(
        appointment
    );
}

private Payment createPendingPayment(
    Appointment appointment
) {
    Payment payment = new Payment();

    payment.setAppointment(appointment);
    payment.setStatus(PaymentStatus.PAYMENT_PENDING);
    payment.setAmount(new BigDecimal("700.00"));
    payment.setCurrency("INR");
    payment.setRazorpayReferenceId(
        "mediflow-appt-" + appointment.getId()
    );
    payment.setRazorpayPaymentLinkId(
        "plink_test_" + appointment.getId()
    );
    payment.setRazorpayPaymentLinkUrl(
        "https://rzp.io/i/test-"
            + appointment.getId()
    );

    return paymentRepository.saveAndFlush(payment);
}


}
