package com.mediflow.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
class AppointmentRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private DoctorAvailabilitySlotRepository slotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findAllForPatientReturnsOnlyOwnedAppointmentsInDescendingTimeOrder() {
        String uniqueValue = UUID.randomUUID().toString();

        User doctorUser = createUser(
            "Appointment Query Doctor",
            "appointment-doctor-" + uniqueValue + "@example.com",
            Role.DOCTOR
        );

        User firstPatient = createUser(
            "First Appointment Patient",
            "appointment-patient-one-" + uniqueValue + "@example.com",
            Role.PATIENT
        );

        User secondPatient = createUser(
            "Second Appointment Patient",
            "appointment-patient-two-" + uniqueValue + "@example.com",
            Role.PATIENT
        );

        DoctorProfile doctorProfile =
            createDoctorProfile(doctorUser, uniqueValue);

        OffsetDateTime now =
            OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        DoctorAvailabilitySlot pastSlot = createSlot(
            doctorProfile,
            now.minusDays(3),
            now.minusDays(3).plusHours(1)
        );

        DoctorAvailabilitySlot futureSlot = createSlot(
            doctorProfile,
            now.plusDays(3),
            now.plusDays(3).plusHours(1)
        );

        DoctorAvailabilitySlot otherPatientSlot = createSlot(
            doctorProfile,
            now.plusDays(5),
            now.plusDays(5).plusHours(1)
        );

        Appointment pastAppointment = createAppointment(
            pastSlot,
            firstPatient,
            AppointmentStatus.COMPLETED,
            new BigDecimal("500.00"),
            pastSlot.getEndTime()
        );

        Appointment futureAppointment = createAppointment(
            futureSlot,
            firstPatient,
            AppointmentStatus.BOOKED,
            new BigDecimal("750.00"),
            null
        );

        Appointment otherPatientAppointment =
            createAppointment(
                otherPatientSlot,
                secondPatient,
                AppointmentStatus.BOOKED,
                new BigDecimal("750.00"),
                null
            );

        entityManager.flush();
        entityManager.clear();

        List<Appointment> result =
            appointmentRepository.findAllForPatient(
                firstPatient.getId()
            );

        assertThat(result)
            .extracting(Appointment::getId)
            .containsExactly(
                futureAppointment.getId(),
                pastAppointment.getId()
            );

        assertThat(result)
            .extracting(Appointment::getId)
            .doesNotContain(otherPatientAppointment.getId());

        assertThat(result)
            .extracting(Appointment::getStatus)
            .containsExactly(
                AppointmentStatus.BOOKED,
                AppointmentStatus.COMPLETED
            );

        assertThat(
            result.getFirst()
                .getAvailabilitySlot()
                .getDoctorProfile()
                .getUser()
                .getFullName()
        ).isEqualTo("Appointment Query Doctor");
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
            "APPT-TEST-LIC-" + uniqueValue
        );
        doctorProfile.setConsultationFee(
            new BigDecimal("750.00")
        );
        doctorProfile.setBio(
            "Doctor profile created for appointment repository testing."
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
        BigDecimal consultationFee,
        OffsetDateTime completedAt
    ) {
        Appointment appointment = new Appointment();

        appointment.setAvailabilitySlot(slot);
        appointment.setPatient(patient);
        appointment.setStatus(status);
        appointment.setConsultationFeeSnapshot(
            consultationFee
        );
        appointment.setCompletedAt(completedAt);

        return appointmentRepository.saveAndFlush(
            appointment
        );
    }
}