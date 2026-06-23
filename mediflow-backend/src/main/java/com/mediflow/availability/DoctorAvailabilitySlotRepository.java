package com.mediflow.availability;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorAvailabilitySlotRepository
extends JpaRepository<DoctorAvailabilitySlot, Long> {

List<DoctorAvailabilitySlot>
    findAllByDoctorProfileIdAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
        Long doctorProfileId,
        OffsetDateTime startTime
    );

boolean existsByDoctorProfileIdAndStartTimeLessThanAndEndTimeGreaterThan(
    Long doctorProfileId,
    OffsetDateTime proposedEndTime,
    OffsetDateTime proposedStartTime
);


}
