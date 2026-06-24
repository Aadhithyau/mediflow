package com.mediflow.appointment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
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
class PatientAppointmentCancellationIntegrationTest {

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
    void patientCancelsFutureAppointmentAndSlotCanBeRebooked()
        throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        User doctor = createUser(
            "Cancellation Doctor",
            "cancellation-doctor-" + uniqueValue
                + "@example.com",
            Role.DOCTOR
        );

        User firstPatient = createUser(
            "First Cancellation Patient",
            "first-cancellation-patient-" + uniqueValue
                + "@example.com",
            Role.PATIENT
        );

        User secondPatient = createUser(
            "Second Cancellation Patient",
            "second-cancellation-patient-" + uniqueValue
                + "@example.com",
            Role.PATIENT
        );

        DoctorProfile doctorProfile = createDoctorProfile(
            doctor,
            uniqueValue
        );

        OffsetDateTime startTime =
            OffsetDateTime.now(ZoneOffset.UTC)
                .withNano(0)
                .plusDays(2);

        DoctorAvailabilitySlot slot = createSlot(
            doctorProfile,
            startTime,
            startTime.plusHours(1)
        );

        Appointment originalAppointment = createAppointment(
            slot,
            firstPatient,
            AppointmentStatus.BOOKED,
            null
        );

        String firstPatientToken =
            jwtService.generateAccessToken(firstPatient);

        String cancellationEndpoint =
            "/api/appointments/"
                + originalAppointment.getId()
                + "/cancel";

        mockMvc.perform(
            patch(cancellationEndpoint)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + firstPatientToken
                )
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.id")
                    .value(originalAppointment.getId())
            )
            .andExpect(
                jsonPath("$.availabilitySlotId")
                    .value(slot.getId())
            )
            .andExpect(
                jsonPath("$.status")
                    .value("CANCELLED")
            );

        entityManager.flush();
        entityManager.clear();

        Appointment cancelledAppointment =
            appointmentRepository
                .findById(originalAppointment.getId())
                .orElseThrow();

        assertThat(cancelledAppointment.getStatus())
            .isEqualTo(AppointmentStatus.CANCELLED);

        assertThat(cancelledAppointment.getCompletedAt())
            .isNull();

        mockMvc.perform(
            patch(cancellationEndpoint)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + firstPatientToken
                )
        )
            .andExpect(status().isConflict());

        String secondPatientToken =
            jwtService.generateAccessToken(secondPatient);

        mockMvc.perform(
            get(
                "/api/doctors/"
                    + doctorProfile.getId()
                    + "/availability-slots"
            )
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + secondPatientToken
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(
                jsonPath("$[0].id")
                    .value(slot.getId())
            );

        mockMvc.perform(
            post("/api/appointments")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + secondPatientToken
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "availabilitySlotId": %d
                    }
                    """.formatted(slot.getId())
                )
        )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.availabilitySlotId")
                    .value(slot.getId())
            )
            .andExpect(
                jsonPath("$.status")
                    .value("BOOKED")
            );

        entityManager.flush();
        entityManager.clear();

        assertThat(
            appointmentRepository
                .findAllForPatient(secondPatient.getId())
        )
            .hasSize(1)
            .extracting(Appointment::getStatus)
            .containsExactly(AppointmentStatus.BOOKED);
    }

    @Test
    void differentPatientCannotCancelAnotherPatientsAppointment()
        throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        User doctor = createUser(
            "Ownership Cancellation Doctor",
            "ownership-cancel-doctor-" + uniqueValue
                + "@example.com",
            Role.DOCTOR
        );

        User ownerPatient = createUser(
            "Owner Cancellation Patient",
            "owner-cancel-patient-" + uniqueValue
                + "@example.com",
            Role.PATIENT
        );

        User otherPatient = createUser(
            "Other Cancellation Patient",
            "other-cancel-patient-" + uniqueValue
                + "@example.com",
            Role.PATIENT
        );

        DoctorProfile doctorProfile = createDoctorProfile(
            doctor,
            "OWNER-" + uniqueValue
        );

        OffsetDateTime startTime =
            OffsetDateTime.now(ZoneOffset.UTC)
                .withNano(0)
                .plusDays(2);

        DoctorAvailabilitySlot slot = createSlot(
            doctorProfile,
            startTime,
            startTime.plusHours(1)
        );

        Appointment appointment = createAppointment(
            slot,
            ownerPatient,
            AppointmentStatus.BOOKED,
            null
        );

        String otherPatientToken =
            jwtService.generateAccessToken(otherPatient);

        mockMvc.perform(
            patch(
                "/api/appointments/"
                    + appointment.getId()
                    + "/cancel"
            )
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + otherPatientToken
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
    }

    @Test
    void patientCannotCancelAppointmentAfterStartTime()
        throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        User doctor = createUser(
            "Past Cancellation Doctor",
            "past-cancel-doctor-" + uniqueValue
                + "@example.com",
            Role.DOCTOR
        );

        User patient = createUser(
            "Past Cancellation Patient",
            "past-cancel-patient-" + uniqueValue
                + "@example.com",
            Role.PATIENT
        );

        DoctorProfile doctorProfile = createDoctorProfile(
            doctor,
            "PAST-" + uniqueValue
        );

        OffsetDateTime startTime =
            OffsetDateTime.now(ZoneOffset.UTC)
                .withNano(0)
                .minusDays(1);

        DoctorAvailabilitySlot slot = createSlot(
            doctorProfile,
            startTime,
            startTime.plusHours(1)
        );

        Appointment appointment = createAppointment(
            slot,
            patient,
            AppointmentStatus.BOOKED,
            null
        );

        String patientToken =
            jwtService.generateAccessToken(patient);

        mockMvc.perform(
            patch(
                "/api/appointments/"
                    + appointment.getId()
                    + "/cancel"
            )
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + patientToken
                )
        )
            .andExpect(status().isConflict());

        entityManager.flush();
        entityManager.clear();

        Appointment unchangedAppointment =
            appointmentRepository
                .findById(appointment.getId())
                .orElseThrow();

        assertThat(unchangedAppointment.getStatus())
            .isEqualTo(AppointmentStatus.BOOKED);
    }

    @Test
    void patientCannotCancelCompletedAppointment()
        throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        User doctor = createUser(
            "Completed Cancellation Doctor",
            "completed-cancel-doctor-" + uniqueValue
                + "@example.com",
            Role.DOCTOR
        );

        User patient = createUser(
            "Completed Cancellation Patient",
            "completed-cancel-patient-" + uniqueValue
                + "@example.com",
            Role.PATIENT
        );

        DoctorProfile doctorProfile = createDoctorProfile(
            doctor,
            "COMPLETED-" + uniqueValue
        );

        OffsetDateTime startTime =
            OffsetDateTime.now(ZoneOffset.UTC)
                .withNano(0)
                .plusDays(2);

        DoctorAvailabilitySlot slot = createSlot(
            doctorProfile,
            startTime,
            startTime.plusHours(1)
        );

        Appointment appointment = createAppointment(
            slot,
            patient,
            AppointmentStatus.COMPLETED,
            OffsetDateTime.now(ZoneOffset.UTC)
        );

        String patientToken =
            jwtService.generateAccessToken(patient);

        mockMvc.perform(
            patch(
                "/api/appointments/"
                    + appointment.getId()
                    + "/cancel"
            )
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + patientToken
                )
        )
            .andExpect(status().isConflict());
    }

    @Test
    void doctorAndUnauthenticatedUserCannotCancelPatientAppointment()
        throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        User doctor = createUser(
            "Blocked Cancellation Doctor",
            "blocked-cancel-doctor-" + uniqueValue
                + "@example.com",
            Role.DOCTOR
        );

        String doctorToken =
            jwtService.generateAccessToken(doctor);

        mockMvc.perform(
            patch("/api/appointments/1/cancel")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + doctorToken
                )
        )
            .andExpect(status().isForbidden());

        mockMvc.perform(
            patch("/api/appointments/1/cancel")
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
        DoctorProfile doctorProfile =
            new DoctorProfile();

        doctorProfile.setUser(doctor);
        doctorProfile.setSpecialization(
            "General Medicine"
        );
        doctorProfile.setMedicalLicenseNumber(
            "CANCEL-TEST-" + licenseSuffix
        );
        doctorProfile.setConsultationFee(
            new BigDecimal("600.00")
        );
        doctorProfile.setBio(
            "Doctor profile created for cancellation integration testing."
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
        AppointmentStatus status,
        OffsetDateTime completedAt
    ) {
        Appointment appointment = new Appointment();

        appointment.setAvailabilitySlot(slot);
        appointment.setPatient(patient);
        appointment.setStatus(status);
        appointment.setConsultationFeeSnapshot(
            new BigDecimal("600.00")
        );
        appointment.setCompletedAt(completedAt);

        return appointmentRepository.saveAndFlush(
            appointment
        );
    }
}