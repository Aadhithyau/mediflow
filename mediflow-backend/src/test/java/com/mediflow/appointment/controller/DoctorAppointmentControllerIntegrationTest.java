package com.mediflow.appointment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DoctorAppointmentControllerIntegrationTest {

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

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void doctorReceivesOnlyOwnedAppointmentsInDescendingOrder()
        throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        User firstDoctor = createUser(
            "First Appointment Doctor",
            "first-doctor-" + uniqueValue + "@example.com",
            Role.DOCTOR
        );

        User secondDoctor = createUser(
            "Second Appointment Doctor",
            "second-doctor-" + uniqueValue + "@example.com",
            Role.DOCTOR
        );

        User patient = createUser(
            "Doctor Endpoint Patient",
            "doctor-endpoint-patient-" + uniqueValue
                + "@example.com",
            Role.PATIENT
        );

        DoctorProfile firstDoctorProfile = createDoctorProfile(
            firstDoctor,
            "FIRST-" + uniqueValue
        );

        DoctorProfile secondDoctorProfile = createDoctorProfile(
            secondDoctor,
            "SECOND-" + uniqueValue
        );

        OffsetDateTime now =
            OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        DoctorAvailabilitySlot olderSlot = createSlot(
            firstDoctorProfile,
            now.plusDays(1),
            now.plusDays(1).plusHours(1)
        );

        DoctorAvailabilitySlot newerSlot = createSlot(
            firstDoctorProfile,
            now.plusDays(3),
            now.plusDays(3).plusHours(1)
        );

        DoctorAvailabilitySlot otherDoctorSlot = createSlot(
            secondDoctorProfile,
            now.plusDays(2),
            now.plusDays(2).plusHours(1)
        );

        Appointment olderAppointment = createAppointment(
            olderSlot,
            patient
        );

        Appointment newerAppointment = createAppointment(
            newerSlot,
            patient
        );

        createAppointment(
            otherDoctorSlot,
            patient
        );

        String accessToken =
            jwtService.generateAccessToken(firstDoctor);

        mockMvc.perform(
            get("/api/doctor/appointments")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + accessToken
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(
                jsonPath("$[0].id")
                    .value(newerAppointment.getId())
            )
            .andExpect(
                jsonPath("$[0].availabilitySlotId")
                    .value(newerSlot.getId())
            )
            .andExpect(
                jsonPath("$[0].patientUserId")
                    .value(patient.getId())
            )
            .andExpect(
                jsonPath("$[0].patientName")
                    .value("Doctor Endpoint Patient")
            )
            .andExpect(
                jsonPath("$[0].status")
                    .value("BOOKED")
            )
            .andExpect(
                jsonPath("$[1].id")
                    .value(olderAppointment.getId())
            )
            .andExpect(
                jsonPath("$[1].availabilitySlotId")
                    .value(olderSlot.getId())
            );
    }

    @Test
    void doctorCompletesOwnedAppointmentOnlyOnce()
        throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        User doctor = createUser(
            "Completion Doctor",
            "completion-doctor-" + uniqueValue + "@example.com",
            Role.DOCTOR
        );

        User patient = createUser(
            "Completion Patient",
            "completion-patient-" + uniqueValue + "@example.com",
            Role.PATIENT
        );

        DoctorProfile doctorProfile = createDoctorProfile(
            doctor,
            "COMPLETE-" + uniqueValue
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

        Appointment appointment = createAppointment(
            slot,
            patient
        );

        String accessToken =
            jwtService.generateAccessToken(doctor);

        String endpoint =
            "/api/doctor/appointments/"
                + appointment.getId()
                + "/complete";

        mockMvc.perform(
            patch(endpoint)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + accessToken
                )
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.id")
                    .value(appointment.getId())
            )
            .andExpect(
                jsonPath("$.status")
                    .value("COMPLETED")
            )
            .andExpect(
                jsonPath("$.completedAt")
                    .value(notNullValue())
            );

        entityManager.flush();
        entityManager.clear();

        Appointment completedAppointment =
            appointmentRepository
                .findById(appointment.getId())
                .orElseThrow();

        assertThat(completedAppointment.getStatus())
            .isEqualTo(AppointmentStatus.COMPLETED);

        assertThat(completedAppointment.getCompletedAt())
            .isNotNull();

        mockMvc.perform(
            patch(endpoint)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + accessToken
                )
        )
            .andExpect(status().isConflict());
    }

    @Test
    void differentDoctorCannotCompleteAnotherDoctorsAppointment()
        throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        User ownerDoctor = createUser(
            "Owner Doctor",
            "owner-doctor-" + uniqueValue + "@example.com",
            Role.DOCTOR
        );

        User otherDoctor = createUser(
            "Other Doctor",
            "other-doctor-" + uniqueValue + "@example.com",
            Role.DOCTOR
        );

        User patient = createUser(
            "Ownership Patient",
            "ownership-patient-" + uniqueValue + "@example.com",
            Role.PATIENT
        );

        DoctorProfile ownerProfile = createDoctorProfile(
            ownerDoctor,
            "OWNER-" + uniqueValue
        );

        OffsetDateTime startTime =
            OffsetDateTime.now(ZoneOffset.UTC)
                .withNano(0)
                .plusDays(1);

        DoctorAvailabilitySlot slot = createSlot(
            ownerProfile,
            startTime,
            startTime.plusHours(1)
        );

        Appointment appointment = createAppointment(
            slot,
            patient
        );

        String otherDoctorToken =
            jwtService.generateAccessToken(otherDoctor);

        mockMvc.perform(
            patch(
                "/api/doctor/appointments/"
                    + appointment.getId()
                    + "/complete"
            )
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + otherDoctorToken
                )
        )
            .andExpect(status().isNotFound());

        entityManager.flush();
        entityManager.clear();

        Appointment unchangedAppointment =
            appointmentRepository
                .findById(appointment.getId())
                .orElseThrow();

        assertThat(unchangedAppointment.getStatus())
            .isEqualTo(AppointmentStatus.BOOKED);

        assertThat(unchangedAppointment.getCompletedAt())
            .isNull();
    }

    @Test
    void patientCannotAccessDoctorAppointmentEndpoints()
        throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        User patient = createUser(
            "Blocked Patient",
            "blocked-patient-" + uniqueValue + "@example.com",
            Role.PATIENT
        );

        String accessToken =
            jwtService.generateAccessToken(patient);

        mockMvc.perform(
            get("/api/doctor/appointments")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + accessToken
                )
        )
            .andExpect(status().isForbidden());

        mockMvc.perform(
            patch("/api/doctor/appointments/1/complete")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + accessToken
                )
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestsAreRejected()
        throws Exception {

        mockMvc.perform(
            get("/api/doctor/appointments")
        )
            .andExpect(status().isUnauthorized());

        mockMvc.perform(
            patch("/api/doctor/appointments/1/complete")
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
        String licenseSuffix
    ) {
        DoctorProfile doctorProfile = new DoctorProfile();

        doctorProfile.setUser(doctor);
        doctorProfile.setSpecialization(
            "General Medicine"
        );
        doctorProfile.setMedicalLicenseNumber(
            "DOCTOR-APPT-" + licenseSuffix
        );
        doctorProfile.setConsultationFee(
            new BigDecimal("700.00")
        );
        doctorProfile.setBio(
            "Doctor profile created for appointment operation testing."
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
        User patient
    ) {
        Appointment appointment = new Appointment();

        appointment.setAvailabilitySlot(slot);
        appointment.setPatient(patient);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setConsultationFeeSnapshot(
            new BigDecimal("700.00")
        );

        return appointmentRepository.saveAndFlush(
            appointment
        );
    }
}