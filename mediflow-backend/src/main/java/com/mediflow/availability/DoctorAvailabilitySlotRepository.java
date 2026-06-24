package com.mediflow.availability;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediflow.appointment.AppointmentStatus;

public interface DoctorAvailabilitySlotRepository
    extends JpaRepository<DoctorAvailabilitySlot, Long> {

    @Query("""
        SELECT slot
        FROM DoctorAvailabilitySlot slot
        WHERE slot.doctorProfile.id = :doctorProfileId
          AND slot.startTime >= :startTime
          AND NOT EXISTS (
              SELECT appointment.id
              FROM Appointment appointment
              WHERE appointment.availabilitySlot.id = slot.id
                AND appointment.status <> :cancelledStatus
          )
        ORDER BY slot.startTime ASC
        """)
    List<DoctorAvailabilitySlot> findAvailableFutureSlots(
        @Param("doctorProfileId") Long doctorProfileId,
        @Param("startTime") OffsetDateTime startTime,
        @Param("cancelledStatus")
            AppointmentStatus cancelledStatus
    );

    boolean
        existsByDoctorProfileIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long doctorProfileId,
            OffsetDateTime proposedEndTime,
            OffsetDateTime proposedStartTime
        );
}