package com.mediflow.availability.controller;

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
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DoctorAvailabilityControllerIntegrationTest {

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
private JwtService jwtService;

@Test
void doctorReceivesOnlyOwnAvailableFutureSlots()
    throws Exception {

    String uniqueValue = UUID.randomUUID().toString();

    User firstDoctor = createUser(
        "First Availability Doctor",
        "first-availability-doctor-"
            + uniqueValue
            + "@example.com",
        Role.DOCTOR
    );

    User secondDoctor = createUser(
        "Second Availability Doctor",
        "second-availability-doctor-"
            + uniqueValue
            + "@example.com",
        Role.DOCTOR
    );

    User patient = createUser(
        "Availability Patient",
        "availability-patient-"
            + uniqueValue
            + "@example.com",
        Role.PATIENT
    );

    DoctorProfile firstDoctorProfile =
        createDoctorProfile(
            firstDoctor,
            "FIRST-" + uniqueValue
        );

    DoctorProfile secondDoctorProfile =
        createDoctorProfile(
            secondDoctor,
            "SECOND-" + uniqueValue
        );

    OffsetDateTime now =
        OffsetDateTime.now(ZoneOffset.UTC)
            .withNano(0);

    DoctorAvailabilitySlot availableSlot =
        createSlot(
            firstDoctorProfile,
            now.plusDays(1),
            now.plusDays(1).plusHours(1)
        );

    DoctorAvailabilitySlot bookedSlot =
        createSlot(
            firstDoctorProfile,
            now.plusDays(2),
            now.plusDays(2).plusHours(1)
        );

    DoctorAvailabilitySlot cancelledSlot =
        createSlot(
            firstDoctorProfile,
            now.plusDays(3),
            now.plusDays(3).plusHours(1)
        );

    createSlot(
        secondDoctorProfile,
        now.plusDays(4),
        now.plusDays(4).plusHours(1)
    );

    createAppointment(
        bookedSlot,
        patient,
        AppointmentStatus.BOOKED
    );

    createAppointment(
        cancelledSlot,
        patient,
        AppointmentStatus.CANCELLED
    );

    String accessToken =
        jwtService.generateAccessToken(firstDoctor);

    mockMvc.perform(
        get("/api/doctor/availability-slots")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + accessToken
            )
    )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(
            jsonPath("$[0].id")
                .value(availableSlot.getId())
        )
        .andExpect(
            jsonPath("$[0].doctorProfileId")
                .value(firstDoctorProfile.getId())
        )
        .andExpect(
            jsonPath("$[1].id")
                .value(cancelledSlot.getId())
        )
        .andExpect(
            jsonPath("$[1].doctorProfileId")
                .value(firstDoctorProfile.getId())
        );
}

@Test
void patientCannotAccessDoctorAvailabilityEndpoint()
    throws Exception {

    String uniqueValue = UUID.randomUUID().toString();

    User patient = createUser(
        "Forbidden Availability Patient",
        "forbidden-availability-patient-"
            + uniqueValue
            + "@example.com",
        Role.PATIENT
    );

    String accessToken =
        jwtService.generateAccessToken(patient);

    mockMvc.perform(
        get("/api/doctor/availability-slots")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + accessToken
            )
    )
        .andExpect(status().isForbidden());
}

@Test
void unauthenticatedUserCannotAccessDoctorAvailabilityEndpoint()
    throws Exception {

    mockMvc.perform(
        get("/api/doctor/availability-slots")
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
    User doctorUser,
    String licenseSuffix
) {
    DoctorProfile doctorProfile =
        new DoctorProfile();

    doctorProfile.setUser(doctorUser);
    doctorProfile.setSpecialization(
        "General Medicine"
    );
    doctorProfile.setMedicalLicenseNumber(
        "AVAILABILITY-" + licenseSuffix
    );
    doctorProfile.setConsultationFee(
        new BigDecimal("500.00")
    );
    doctorProfile.setBio(
        "Doctor profile created for availability endpoint testing."
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

private Appointment createAppointment(
    DoctorAvailabilitySlot slot,
    User patient,
    AppointmentStatus status
) {
    Appointment appointment = new Appointment();

    appointment.setAvailabilitySlot(slot);
    appointment.setPatient(patient);
    appointment.setStatus(status);
    appointment.setConsultationFeeSnapshot(
        slot.getDoctorProfile()
            .getConsultationFee()
    );

    return appointmentRepository.saveAndFlush(
        appointment
    );
}

}