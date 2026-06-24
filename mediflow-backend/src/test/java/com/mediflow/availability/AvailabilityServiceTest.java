package com.mediflow.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

import com.mediflow.availability.dto.AvailabilitySlotResponse;
import com.mediflow.doctor.DoctorProfile;
import com.mediflow.doctor.DoctorProfileRepository;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private DoctorAvailabilitySlotRepository slotRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void getFutureSlotsUsesAvailableSlotQueryAndMapsResults() {
        Long doctorProfileId = 1L;

        User doctorUser = mock(User.class);
        DoctorProfile doctorProfile = mock(DoctorProfile.class);

        DoctorAvailabilitySlot earlierSlot =
            mock(DoctorAvailabilitySlot.class);

        DoctorAvailabilitySlot laterSlot =
            mock(DoctorAvailabilitySlot.class);

        OffsetDateTime earlierStart =
            OffsetDateTime.parse("2030-01-10T09:00:00Z");

        OffsetDateTime earlierEnd =
            OffsetDateTime.parse("2030-01-10T10:00:00Z");

        OffsetDateTime laterStart =
            OffsetDateTime.parse("2030-01-10T11:00:00Z");

        OffsetDateTime laterEnd =
            OffsetDateTime.parse("2030-01-10T12:00:00Z");

        when(doctorProfileRepository.findById(doctorProfileId))
            .thenReturn(Optional.of(doctorProfile));

        when(doctorProfile.getId())
            .thenReturn(doctorProfileId);

        when(doctorProfile.getUser())
            .thenReturn(doctorUser);

        when(doctorUser.isEnabled())
            .thenReturn(true);

        when(doctorUser.getRole())
            .thenReturn(Role.DOCTOR);

        when(earlierSlot.getId())
            .thenReturn(11L);

        when(earlierSlot.getStartTime())
            .thenReturn(earlierStart);

        when(earlierSlot.getEndTime())
            .thenReturn(earlierEnd);

        when(laterSlot.getId())
            .thenReturn(12L);

        when(laterSlot.getStartTime())
            .thenReturn(laterStart);

        when(laterSlot.getEndTime())
            .thenReturn(laterEnd);

        when(slotRepository.findAvailableFutureSlots(
            eq(doctorProfileId),
            any(OffsetDateTime.class)
        )).thenReturn(List.of(earlierSlot, laterSlot));

        List<AvailabilitySlotResponse> result =
            availabilityService.getFutureSlots(doctorProfileId);

        assertThat(result).containsExactly(
            new AvailabilitySlotResponse(
                11L,
                doctorProfileId,
                earlierStart,
                earlierEnd
            ),
            new AvailabilitySlotResponse(
                12L,
                doctorProfileId,
                laterStart,
                laterEnd
            )
        );

        verify(slotRepository).findAvailableFutureSlots(
            eq(doctorProfileId),
            any(OffsetDateTime.class)
        );
    }

    @Test
    void getFutureSlotsRejectsDisabledDoctorProfile() {
        Long doctorProfileId = 2L;

        User disabledDoctorUser = mock(User.class);
        DoctorProfile doctorProfile = mock(DoctorProfile.class);

        when(doctorProfileRepository.findById(doctorProfileId))
            .thenReturn(Optional.of(doctorProfile));

        when(doctorProfile.getUser())
            .thenReturn(disabledDoctorUser);

        when(disabledDoctorUser.isEnabled())
            .thenReturn(false);

        assertThatThrownBy(
            () -> availabilityService.getFutureSlots(doctorProfileId)
        )
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(exception -> {
                ResponseStatusException responseException =
                    (ResponseStatusException) exception;

                assertThat(responseException.getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

                assertThat(responseException.getReason())
                    .isEqualTo("Doctor profile was not found");
            });

        verifyNoInteractions(slotRepository);
    }
}