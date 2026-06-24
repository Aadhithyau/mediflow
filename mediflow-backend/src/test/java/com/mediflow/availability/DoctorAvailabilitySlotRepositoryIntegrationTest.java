package com.mediflow.availability;

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

import com.mediflow.appointment.Appointment;
import com.mediflow.appointment.AppointmentRepository;
import com.mediflow.appointment.AppointmentStatus;
import com.mediflow.doctor.DoctorProfile;
import com.mediflow.doctor.DoctorProfileRepository;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@SpringBootTest
@Transactional
class DoctorAvailabilitySlotRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private DoctorAvailabilitySlotRepository slotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void findAvailableFutureSlotsExcludesBookedAndPastSlotsAndOrdersResults() {
        String uniqueValue = UUID.randomUUID().toString();

        User doctorUser = createUser(
            "Availability Test Doctor",
            "availability-doctor-" + uniqueValue + "@example.com",
            Role.DOCTOR
        );

        User patientUser = createUser(
            "Availability Test Patient",
            "availability-patient-" + uniqueValue + "@example.com",
            Role.PATIENT
        );

        DoctorProfile doctorProfile =
            createDoctorProfile(doctorUser, uniqueValue);

        OffsetDateTime now =
            OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        DoctorAvailabilitySlot pastSlot = createSlot(
            doctorProfile,
            now.minusDays(2),
            now.minusDays(2).plusHours(1)
        );

        DoctorAvailabilitySlot earlierAvailableSlot = createSlot(
            doctorProfile,
            now.plusDays(1),
            now.plusDays(1).plusHours(1)
        );

        DoctorAvailabilitySlot bookedSlot = createSlot(
            doctorProfile,
            now.plusDays(2),
            now.plusDays(2).plusHours(1)
        );

        DoctorAvailabilitySlot laterAvailableSlot = createSlot(
            doctorProfile,
            now.plusDays(3),
            now.plusDays(3).plusHours(1)
        );

        createAppointment(
            bookedSlot,
            patientUser,
            doctorProfile.getConsultationFee()
        );

        List<DoctorAvailabilitySlot> result =
            slotRepository.findAvailableFutureSlots(
                doctorProfile.getId(),
                now
            );

        assertThat(result)
            .extracting(DoctorAvailabilitySlot::getId)
            .containsExactly(
                earlierAvailableSlot.getId(),
                laterAvailableSlot.getId()
            );

        assertThat(result)
            .extracting(DoctorAvailabilitySlot::getId)
            .doesNotContain(
                pastSlot.getId(),
                bookedSlot.getId()
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
        user.setPasswordHash("integration-test-password-hash");
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
        doctorProfile.setSpecialization("General Medicine");
        doctorProfile.setMedicalLicenseNumber(
            "TEST-LIC-" + uniqueValue
        );
        doctorProfile.setConsultationFee(
            new BigDecimal("500.00")
        );
        doctorProfile.setBio(
            "Doctor profile created for availability integration testing."
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
        User patientUser,
        BigDecimal consultationFee
    ) {
        Appointment appointment = new Appointment();

        appointment.setAvailabilitySlot(slot);
        appointment.setPatient(patientUser);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setConsultationFeeSnapshot(
            consultationFee
        );

        return appointmentRepository.saveAndFlush(
            appointment
        );
    }
}