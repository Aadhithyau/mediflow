package com.mediflow.appointment;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.appointment.dto.AppointmentResponse;
import com.mediflow.appointment.dto.CreateAppointmentRequest;
import com.mediflow.availability.DoctorAvailabilitySlot;
import com.mediflow.availability.DoctorAvailabilitySlotRepository;
import com.mediflow.doctor.DoctorProfile;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@Service
public class AppointmentService {

    private static final String DUPLICATE_BOOKING_CONSTRAINT =
        "uq_appointments_availability_slot";

    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilitySlotRepository slotRepository;
    private final UserRepository userRepository;

    public AppointmentService(
        AppointmentRepository appointmentRepository,
        DoctorAvailabilitySlotRepository slotRepository,
        UserRepository userRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AppointmentResponse bookAppointment(
        String patientEmail,
        CreateAppointmentRequest request
    ) {
        User patient = userRepository.findByEmail(patientEmail)
            .filter(User::isEnabled)
            .filter(user -> user.getRole() == Role.PATIENT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Patient account is unavailable"
            ));

        DoctorAvailabilitySlot slot = slotRepository
            .findById(request.availabilitySlotId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Availability slot was not found"
            ));

        DoctorProfile doctorProfile = slot.getDoctorProfile();
        User doctorUser = doctorProfile.getUser();

        if (
            !doctorUser.isEnabled()
                || doctorUser.getRole() != Role.DOCTOR
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Doctor is unavailable for booking"
            );
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (!slot.getStartTime().isAfter(now)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Availability slot is no longer bookable"
            );
        }

        boolean alreadyBooked = appointmentRepository
            .existsByAvailabilitySlot_Id(slot.getId());

        if (alreadyBooked) {
            throw bookingConflict();
        }

        Appointment appointment = new Appointment();

        appointment.setAvailabilitySlot(slot);
        appointment.setPatient(patient);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setConsultationFeeSnapshot(
            doctorProfile.getConsultationFee()
        );

        Appointment savedAppointment;

        try {
            savedAppointment =
                appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateBooking(exception)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Availability slot has already been booked",
                    exception
                );
            }

            throw exception;
        }

        return new AppointmentResponse(
            savedAppointment.getId(),
            slot.getId(),
            doctorProfile.getId(),
            doctorUser.getFullName(),
            slot.getStartTime(),
            slot.getEndTime(),
            savedAppointment.getConsultationFeeSnapshot(),
            savedAppointment.getStatus(),
            savedAppointment.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(
        String patientEmail
    ) {
        User patient = userRepository.findByEmail(patientEmail)
            .filter(User::isEnabled)
            .filter(user -> user.getRole() == Role.PATIENT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Patient account is unavailable"
            ));

        return appointmentRepository
            .findAllForPatient(patient.getId())
            .stream()
            .map(appointment -> {
                DoctorAvailabilitySlot slot =
                    appointment.getAvailabilitySlot();

                DoctorProfile doctorProfile =
                    slot.getDoctorProfile();

                User doctorUser = doctorProfile.getUser();

                return new AppointmentResponse(
                    appointment.getId(),
                    slot.getId(),
                    doctorProfile.getId(),
                    doctorUser.getFullName(),
                    slot.getStartTime(),
                    slot.getEndTime(),
                    appointment.getConsultationFeeSnapshot(),
                    appointment.getStatus(),
                    appointment.getCreatedAt()
                );
            })
            .toList();
    }

    private ResponseStatusException bookingConflict() {
        return new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Availability slot has already been booked"
        );
    }

    private boolean isDuplicateBooking(
        DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return DUPLICATE_BOOKING_CONSTRAINT.equals(
                    violation.getConstraintName()
                );
            }

            cause = cause.getCause();
        }

        return false;
    }
}