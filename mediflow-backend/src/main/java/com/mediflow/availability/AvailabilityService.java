package com.mediflow.availability;

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

@Service
public class AvailabilityService {

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
        doctorProfileRepository.findByUserId(
            doctorUser.getId()
        )
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
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Availability slot overlaps an existing slot"
        );
    }

    DoctorAvailabilitySlot slot =
        new DoctorAvailabilitySlot();

    slot.setDoctorProfile(doctorProfile);
    slot.setStartTime(request.startTime());
    slot.setEndTime(request.endTime());

    DoctorAvailabilitySlot savedSlot =
        slotRepository.save(slot);

    return new AvailabilitySlotResponse(
        savedSlot.getId(),
        doctorProfile.getId(),
        savedSlot.getStartTime(),
        savedSlot.getEndTime()
    );
}


}
