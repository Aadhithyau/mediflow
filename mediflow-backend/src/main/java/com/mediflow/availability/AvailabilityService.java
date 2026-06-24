package com.mediflow.availability;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.availability.dto.AvailabilitySlotResponse;
import com.mediflow.availability.dto.CreateAvailabilitySlotRequest;
import com.mediflow.doctor.DoctorProfile;
import com.mediflow.doctor.DoctorProfileRepository;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AvailabilityService {

    private static final String OVERLAP_CONSTRAINT =
        "ex_doctor_availability_no_overlap";

    private static final String DUPLICATE_SLOT_CONSTRAINT =
        "uq_doctor_availability_slot";

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorAvailabilitySlotRepository slotRepository;

    public AvailabilityService(
        UserRepository userRepository,
        DoctorProfileRepository doctorProfileRepository,
        DoctorAvailabilitySlotRepository slotRepository
    ) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.slotRepository = slotRepository;
    }

    @Transactional
    public AvailabilitySlotResponse createSlot(
        String doctorEmail,
        CreateAvailabilitySlotRequest request
    ) {
        User doctorUser = userRepository.findByEmail(doctorEmail)
            .filter(User::isEnabled)
            .filter(user -> user.getRole() == Role.DOCTOR)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Doctor account is unavailable"
            ));

        DoctorProfile doctorProfile =
            doctorProfileRepository.findByUserId(doctorUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Doctor profile was not found"
                ));

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "End time must be after start time"
            );
        }

        boolean overlaps = slotRepository
            .existsByDoctorProfileIdAndStartTimeLessThanAndEndTimeGreaterThan(
                doctorProfile.getId(),
                request.endTime(),
                request.startTime()
            );

        if (overlaps) {
            throw overlapException();
        }

        DoctorAvailabilitySlot slot =
            new DoctorAvailabilitySlot();

        slot.setDoctorProfile(doctorProfile);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());

        DoctorAvailabilitySlot savedSlot;

        try {
            savedSlot = slotRepository.saveAndFlush(slot);
        } catch (DataIntegrityViolationException exception) {
            if (isAvailabilityConflict(exception)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Availability slot overlaps an existing slot",
                    exception
                );
            }

            throw exception;
        }

        return new AvailabilitySlotResponse(
            savedSlot.getId(),
            doctorProfile.getId(),
            savedSlot.getStartTime(),
            savedSlot.getEndTime()
        );
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getFutureSlots(
        Long doctorProfileId
    ) {
        DoctorProfile doctorProfile =
            doctorProfileRepository.findById(doctorProfileId)
                .filter(profile ->
                    profile.getUser().isEnabled()
                        && profile.getUser().getRole() == Role.DOCTOR
                )
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Doctor profile was not found"
                ));

        return slotRepository
            .findAllByDoctorProfileIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                doctorProfile.getId(),
                OffsetDateTime.now(ZoneOffset.UTC)
            )
            .stream()
            .map(slot -> new AvailabilitySlotResponse(
                slot.getId(),
                doctorProfile.getId(),
                slot.getStartTime(),
                slot.getEndTime()
            ))
            .toList();
    }

    private ResponseStatusException overlapException() {
        return new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Availability slot overlaps an existing slot"
        );
    }

    private boolean isAvailabilityConflict(
        DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraintName = violation.getConstraintName();

                return OVERLAP_CONSTRAINT.equals(constraintName)
                    || DUPLICATE_SLOT_CONSTRAINT.equals(constraintName);
            }

            cause = cause.getCause();
        }

        return false;
    }
}
