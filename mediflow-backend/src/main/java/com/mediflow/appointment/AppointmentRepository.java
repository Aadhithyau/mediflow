package com.mediflow.appointment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository
    extends JpaRepository<Appointment, Long> {

    boolean existsByAvailabilitySlot_IdAndStatusNot(
        Long availabilitySlotId,
        AppointmentStatus excludedStatus
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

    @Query("""
        SELECT appointment
        FROM Appointment appointment
        JOIN FETCH appointment.availabilitySlot slot
        JOIN FETCH slot.doctorProfile doctorProfile
        JOIN FETCH appointment.patient
        WHERE doctorProfile.user.id = :doctorUserId
        ORDER BY slot.startTime DESC
        """)
    List<Appointment> findAllForDoctorUser(
        @Param("doctorUserId") Long doctorUserId
    );

    @Query("""
        SELECT appointment
        FROM Appointment appointment
        JOIN FETCH appointment.availabilitySlot slot
        JOIN FETCH slot.doctorProfile doctorProfile
        JOIN FETCH appointment.patient
        WHERE appointment.id = :appointmentId
          AND doctorProfile.user.id = :doctorUserId
        """)
    Optional<Appointment> findOwnedAppointmentForDoctor(
        @Param("appointmentId") Long appointmentId,
        @Param("doctorUserId") Long doctorUserId
    );

    @Query("""
        SELECT appointment
        FROM Appointment appointment
        JOIN FETCH appointment.availabilitySlot slot
        JOIN FETCH slot.doctorProfile doctorProfile
        JOIN FETCH doctorProfile.user
        WHERE appointment.id = :appointmentId
          AND appointment.patient.id = :patientUserId
        """)
    Optional<Appointment> findOwnedAppointmentForPatient(
        @Param("appointmentId") Long appointmentId,
        @Param("patientUserId") Long patientUserId
    );
}