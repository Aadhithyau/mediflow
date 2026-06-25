package com.mediflow.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.appointment.dto.AppointmentResponse;
import com.mediflow.appointment.dto.DoctorAppointmentResponse;
import com.mediflow.availability.DoctorAvailabilitySlot;
import com.mediflow.availability.DoctorAvailabilitySlotRepository;
import com.mediflow.doctor.DoctorProfile;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorAvailabilitySlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void getPatientAppointmentsMapsRepositoryResultsInExistingOrder() {
        String patientEmail = "patient@example.com";
        Long patientId = 10L;

        User patient = mock(User.class);

        when(userRepository.findByEmail(patientEmail))
            .thenReturn(Optional.of(patient));

        when(patient.isEnabled())
            .thenReturn(true);

        when(patient.getRole())
            .thenReturn(Role.PATIENT);

        when(patient.getId())
            .thenReturn(patientId);

        Appointment newerAppointment = mock(Appointment.class);
        Appointment olderAppointment = mock(Appointment.class);

        DoctorAvailabilitySlot newerSlot =
            mock(DoctorAvailabilitySlot.class);

        DoctorAvailabilitySlot olderSlot =
            mock(DoctorAvailabilitySlot.class);

        DoctorProfile newerDoctorProfile =
            mock(DoctorProfile.class);

        DoctorProfile olderDoctorProfile =
            mock(DoctorProfile.class);

        User newerDoctorUser = mock(User.class);
        User olderDoctorUser = mock(User.class);

        OffsetDateTime newerStart =
            OffsetDateTime.parse("2030-01-15T09:00:00Z");

        OffsetDateTime newerEnd =
            OffsetDateTime.parse("2030-01-15T10:00:00Z");

        OffsetDateTime newerCreatedAt =
            OffsetDateTime.parse("2030-01-01T08:00:00Z");

        OffsetDateTime olderStart =
            OffsetDateTime.parse("2030-01-10T11:00:00Z");

        OffsetDateTime olderEnd =
            OffsetDateTime.parse("2030-01-10T12:00:00Z");

        OffsetDateTime olderCreatedAt =
            OffsetDateTime.parse("2029-12-30T08:00:00Z");

        when(appointmentRepository.findAllForPatient(patientId))
            .thenReturn(List.of(
                newerAppointment,
                olderAppointment
            ));

        when(newerAppointment.getId())
            .thenReturn(101L);

        when(newerAppointment.getAvailabilitySlot())
            .thenReturn(newerSlot);

        when(newerAppointment.getConsultationFeeSnapshot())
            .thenReturn(new BigDecimal("750.00"));

        when(newerAppointment.getStatus())
            .thenReturn(AppointmentStatus.BOOKED);

        when(newerAppointment.getCreatedAt())
            .thenReturn(newerCreatedAt);

        when(newerSlot.getId())
            .thenReturn(201L);

        when(newerSlot.getDoctorProfile())
            .thenReturn(newerDoctorProfile);

        when(newerSlot.getStartTime())
            .thenReturn(newerStart);

        when(newerSlot.getEndTime())
            .thenReturn(newerEnd);

        when(newerDoctorProfile.getId())
            .thenReturn(301L);

        when(newerDoctorProfile.getUser())
            .thenReturn(newerDoctorUser);

        when(newerDoctorUser.getFullName())
            .thenReturn("Dr. Newer");

        when(olderAppointment.getId())
            .thenReturn(102L);

        when(olderAppointment.getAvailabilitySlot())
            .thenReturn(olderSlot);

        when(olderAppointment.getConsultationFeeSnapshot())
            .thenReturn(new BigDecimal("500.00"));

        when(olderAppointment.getStatus())
            .thenReturn(AppointmentStatus.COMPLETED);

        when(olderAppointment.getCreatedAt())
            .thenReturn(olderCreatedAt);

        when(olderSlot.getId())
            .thenReturn(202L);

        when(olderSlot.getDoctorProfile())
            .thenReturn(olderDoctorProfile);

        when(olderSlot.getStartTime())
            .thenReturn(olderStart);

        when(olderSlot.getEndTime())
            .thenReturn(olderEnd);

        when(olderDoctorProfile.getId())
            .thenReturn(302L);

        when(olderDoctorProfile.getUser())
            .thenReturn(olderDoctorUser);

        when(olderDoctorUser.getFullName())
            .thenReturn("Dr. Older");

        List<AppointmentResponse> result =
            appointmentService.getPatientAppointments(
                patientEmail
            );

        assertThat(result).containsExactly(
            new AppointmentResponse(
                101L,
                201L,
                301L,
                "Dr. Newer",
                newerStart,
                newerEnd,
                new BigDecimal("750.00"),
                AppointmentStatus.BOOKED,
                newerCreatedAt
            ),
            new AppointmentResponse(
                102L,
                202L,
                302L,
                "Dr. Older",
                olderStart,
                olderEnd,
                new BigDecimal("500.00"),
                AppointmentStatus.COMPLETED,
                olderCreatedAt
            )
        );

        verify(appointmentRepository)
            .findAllForPatient(patientId);

        verifyNoInteractions(slotRepository);
    }

    @Test
    void getPatientAppointmentsRejectsDisabledPatient() {
        String patientEmail = "disabled@example.com";

        User disabledPatient = mock(User.class);

        when(userRepository.findByEmail(patientEmail))
            .thenReturn(Optional.of(disabledPatient));

        when(disabledPatient.isEnabled())
            .thenReturn(false);

        assertThatThrownBy(
            () -> appointmentService.getPatientAppointments(
                patientEmail
            )
        )
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(exception -> {
                ResponseStatusException responseException =
                    (ResponseStatusException) exception;

                assertThat(responseException.getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);

                assertThat(responseException.getReason())
                    .isEqualTo(
                        "Patient account is unavailable"
                    );
            });

        verifyNoInteractions(
            appointmentRepository,
            slotRepository
        );
    }

    @Test
    void completeAppointmentRejectsBeforeScheduledStartTime() {
        String doctorEmail = "doctor@example.com";
        Long doctorId = 20L;
        Long appointmentId = 101L;

        OffsetDateTime now =
            OffsetDateTime.parse("2030-01-15T09:00:00Z");

        User doctor = mock(User.class);
        Appointment appointment = mock(Appointment.class);

        DoctorAvailabilitySlot slot =
            mock(DoctorAvailabilitySlot.class);

        when(userRepository.findByEmail(doctorEmail))
            .thenReturn(Optional.of(doctor));

        when(doctor.isEnabled())
            .thenReturn(true);

        when(doctor.getRole())
            .thenReturn(Role.DOCTOR);

        when(doctor.getId())
            .thenReturn(doctorId);

        when(appointmentRepository.findOwnedAppointmentForDoctor(
            appointmentId,
            doctorId
        ))
            .thenReturn(Optional.of(appointment));

        when(appointment.getStatus())
            .thenReturn(AppointmentStatus.BOOKED);

        when(appointment.getAvailabilitySlot())
            .thenReturn(slot);

        when(slot.getStartTime())
            .thenReturn(now.plusMinutes(1));

        when(clock.instant())
            .thenReturn(now.toInstant());

        when(clock.getZone())
            .thenReturn(ZoneOffset.UTC);

        assertThatThrownBy(
            () -> appointmentService.completeAppointment(
                doctorEmail,
                appointmentId
            )
        )
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(exception -> {
                ResponseStatusException responseException =
                    (ResponseStatusException) exception;

                assertThat(responseException.getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

                assertThat(responseException.getReason())
                    .isEqualTo(
                        "Appointment cannot be completed before "
                            + "its scheduled start time"
                    );
            });

        verify(appointmentRepository, never())
            .saveAndFlush(any(Appointment.class));

        verify(appointment, never())
            .setStatus(any(AppointmentStatus.class));

        verify(appointment, never())
            .setCompletedAt(any(OffsetDateTime.class));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void completeAppointmentAllowsCompletionAtScheduledStartTime() {
        String doctorEmail = "doctor@example.com";
        Long doctorId = 20L;
        Long appointmentId = 101L;
        Long slotId = 201L;
        Long patientId = 301L;

        OffsetDateTime startTime =
            OffsetDateTime.parse("2030-01-15T09:00:00Z");

        OffsetDateTime endTime =
            startTime.plusHours(1);

        OffsetDateTime createdAt =
            OffsetDateTime.parse("2030-01-01T08:00:00Z");

        User doctor = mock(User.class);
        User patient = mock(User.class);
        Appointment appointment = mock(Appointment.class);

        DoctorAvailabilitySlot slot =
            mock(DoctorAvailabilitySlot.class);

        when(userRepository.findByEmail(doctorEmail))
            .thenReturn(Optional.of(doctor));

        when(doctor.isEnabled())
            .thenReturn(true);

        when(doctor.getRole())
            .thenReturn(Role.DOCTOR);

        when(doctor.getId())
            .thenReturn(doctorId);

        when(appointmentRepository.findOwnedAppointmentForDoctor(
            appointmentId,
            doctorId
        ))
            .thenReturn(Optional.of(appointment));

        when(appointment.getStatus())
            .thenReturn(
                AppointmentStatus.BOOKED,
                AppointmentStatus.COMPLETED
            );

        when(appointment.getAvailabilitySlot())
            .thenReturn(slot);

        when(appointment.getPatient())
            .thenReturn(patient);

        when(appointment.getId())
            .thenReturn(appointmentId);

        when(appointment.getConsultationFeeSnapshot())
            .thenReturn(new BigDecimal("700.00"));

        when(appointment.getCompletedAt())
            .thenReturn(startTime);

        when(appointment.getCreatedAt())
            .thenReturn(createdAt);

        when(slot.getId())
            .thenReturn(slotId);

        when(slot.getStartTime())
            .thenReturn(startTime);

        when(slot.getEndTime())
            .thenReturn(endTime);

        when(patient.getId())
            .thenReturn(patientId);

        when(patient.getFullName())
            .thenReturn("Test Patient");

        when(clock.instant())
            .thenReturn(startTime.toInstant());

        when(clock.getZone())
            .thenReturn(ZoneOffset.UTC);

        when(appointmentRepository.saveAndFlush(appointment))
            .thenReturn(appointment);

        DoctorAppointmentResponse result =
            appointmentService.completeAppointment(
                doctorEmail,
                appointmentId
            );

        assertThat(result.status())
            .isEqualTo(AppointmentStatus.COMPLETED);

        assertThat(result.completedAt())
            .isEqualTo(startTime);

        verify(appointment)
            .setStatus(AppointmentStatus.COMPLETED);

        verify(appointment)
            .setCompletedAt(startTime);

        verify(appointmentRepository)
            .saveAndFlush(appointment);

        verify(eventPublisher)
            .publishEvent(
                new AppointmentCompletedEvent(appointmentId)
            );
    }
}
