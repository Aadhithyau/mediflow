package com.mediflow.appointment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository
    extends JpaRepository<Appointment, Long> {

    boolean existsByAvailabilitySlot_Id(
        Long availabilitySlotId
    );

    @Query("""
        SELECT appointment
        FROM Appointment appointment
        JOIN FETCH appointment.availabilitySlot slot
        JOIN FETCH slot.doctorProfile doctorProfile
        JOIN FETCH doctorProfile.user
        WHERE appointment.patient.id = :patientUserId
        ORDER BY slot.startTime DESC
        """)
    List<Appointment> findAllForPatient(
        @Param("patientUserId") Long patientUserId
    );
}