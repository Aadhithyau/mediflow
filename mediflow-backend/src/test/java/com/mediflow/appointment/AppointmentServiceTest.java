package com.mediflow.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.appointment.dto.AppointmentResponse;
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
}