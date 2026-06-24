package com.mediflow.appointment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientAppointmentControllerIntegrationTest {

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
    void patientReceivesOnlyTheirOwnAppointments() throws Exception {
        String uniqueValue = UUID.randomUUID().toString();

        User doctorUser = createUser(
            "Appointment Endpoint Doctor",
            "endpoint-doctor-" + uniqueValue + "@example.com",
            Role.DOCTOR
        );

        User firstPatient = createUser(
            "First Endpoint Patient",
            "endpoint-patient-one-" + uniqueValue + "@example.com",
            Role.PATIENT
        );

        User secondPatient = createUser(
            "Second Endpoint Patient",
            "endpoint-patient-two-" + uniqueValue + "@example.com",
            Role.PATIENT
        );

        DoctorProfile doctorProfile =
            createDoctorProfile(doctorUser, uniqueValue);

        OffsetDateTime now =
            OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        DoctorAvailabilitySlot firstPatientSlot = createSlot(
            doctorProfile,
            now.plusDays(1),
            now.plusDays(1).plusHours(1)
        );

        DoctorAvailabilitySlot secondPatientSlot = createSlot(
            doctorProfile,
            now.plusDays(2),
            now.plusDays(2).plusHours(1)
        );

        Appointment firstPatientAppointment = createAppointment(
            firstPatientSlot,
            firstPatient,
            new BigDecimal("650.00")
        );

        createAppointment(
            secondPatientSlot,
            secondPatient,
            new BigDecimal("650.00")
        );

        String accessToken =
            jwtService.generateAccessToken(firstPatient);

        mockMvc.perform(
            get("/api/appointments")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + accessToken
                )
        )
            .andExpect(status().isOk())
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(
                jsonPath("$[0].id")
                    .value(firstPatientAppointment.getId())
            )
            .andExpect(
                jsonPath("$[0].availabilitySlotId")
                    .value(firstPatientSlot.getId())
            )
            .andExpect(
                jsonPath("$[0].doctorProfileId")
                    .value(doctorProfile.getId())
            )
            .andExpect(
                jsonPath("$[0].doctorName")
                    .value("Appointment Endpoint Doctor")
            )
            .andExpect(
                jsonPath("$[0].consultationFee")
                    .value(650.00)
            )
            .andExpect(
                jsonPath("$[0].status")
                    .value("BOOKED")
            );
    }

    @Test
    void doctorCannotAccessPatientAppointmentHistory()
        throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        User doctorUser = createUser(
            "Blocked Appointment Doctor",
            "blocked-doctor-" + uniqueValue + "@example.com",
            Role.DOCTOR
        );

        String accessToken =
            jwtService.generateAccessToken(doctorUser);

        mockMvc.perform(
            get("/api/appointments")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + accessToken
                )
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(
            get("/api/appointments")
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
        String uniqueValue
    ) {
        DoctorProfile doctorProfile = new DoctorProfile();

        doctorProfile.setUser(doctorUser);
        doctorProfile.setSpecialization(
            "General Medicine"
        );
        doctorProfile.setMedicalLicenseNumber(
            "ENDPOINT-TEST-LIC-" + uniqueValue
        );
        doctorProfile.setConsultationFee(
            new BigDecimal("650.00")
        );
        doctorProfile.setBio(
            "Doctor profile created for appointment endpoint testing."
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
        BigDecimal consultationFee
    ) {
        Appointment appointment = new Appointment();

        appointment.setAvailabilitySlot(slot);
        appointment.setPatient(patient);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setConsultationFeeSnapshot(
            consultationFee
        );

        return appointmentRepository.saveAndFlush(
            appointment
        );
    }
}