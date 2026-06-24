package com.mediflow.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository
extends JpaRepository<Appointment, Long> {

    boolean existsByAvailabilitySlot_Id(
        Long availabilitySlotId
    );
}