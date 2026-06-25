package com.mediflow.appointment;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.appointment.dto.AppointmentResponse;
import com.mediflow.appointment.dto.CreateAppointmentRequest;
import com.mediflow.appointment.dto.DoctorAppointmentResponse;
import com.mediflow.availability.DoctorAvailabilitySlot;
import com.mediflow.availability.DoctorAvailabilitySlotRepository;
import com.mediflow.doctor.DoctorProfile;
import com.mediflow.user.Role;
import com.mediflow.user.User;
import com.mediflow.user.UserRepository;

@Service
public class AppointmentService {

    private static final String DUPLICATE_BOOKING_CONSTRAINT =
        "uq_appointments_non_cancelled_slot";

    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilitySlotRepository slotRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AppointmentService(
        AppointmentRepository appointmentRepository,
        DoctorAvailabilitySlotRepository slotRepository,
        UserRepository userRepository,
        ApplicationEventPublisher eventPublisher,
        Clock clock
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public AppointmentResponse bookAppointment(
        String patientEmail,
        CreateAppointmentRequest request
    ) {
        User patient = findEnabledPatient(patientEmail);

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

        OffsetDateTime now = OffsetDateTime.now(clock);

        if (!slot.getStartTime().isAfter(now)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Availability slot is no longer bookable"
            );
        }

        boolean alreadyBooked = appointmentRepository
            .existsByAvailabilitySlot_IdAndStatusNot(
                slot.getId(),
                AppointmentStatus.CANCELLED
            );

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

        return toPatientAppointmentResponse(savedAppointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(
        String patientEmail
    ) {
        User patient = findEnabledPatient(patientEmail);

        return appointmentRepository
            .findAllForPatient(patient.getId())
            .stream()
            .map(this::toPatientAppointmentResponse)
            .toList();
    }

    @Transactional
    public AppointmentResponse cancelAppointment(
        String patientEmail,
        Long appointmentId
    ) {
        User patient = findEnabledPatient(patientEmail);

        Appointment appointment = appointmentRepository
            .findOwnedAppointmentForPatient(
                appointmentId,
                patient.getId()
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Appointment was not found for this patient"
            ));

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only booked appointments can be cancelled"
            );
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        if (
            !appointment.getAvailabilitySlot()
                .getStartTime()
                .isAfter(now)
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Appointment can no longer be cancelled"
            );
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCompletedAt(null);

        Appointment savedAppointment =
            appointmentRepository.saveAndFlush(appointment);

        return toPatientAppointmentResponse(savedAppointment);
    }

    @Transactional(readOnly = true)
    public List<DoctorAppointmentResponse> getDoctorAppointments(
        String doctorEmail
    ) {
        User doctor = findEnabledDoctor(doctorEmail);

        return appointmentRepository
            .findAllForDoctorUser(doctor.getId())
            .stream()
            .map(this::toDoctorAppointmentResponse)
            .toList();
    }

    @Transactional
    public DoctorAppointmentResponse completeAppointment(
        String doctorEmail,
        Long appointmentId
    ) {
        User doctor = findEnabledDoctor(doctorEmail);

        Appointment appointment = appointmentRepository
            .findOwnedAppointmentForDoctor(
                appointmentId,
                doctor.getId()
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Appointment was not found for this doctor"
            ));

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only booked appointments can be completed"
            );
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        if (
            appointment.getAvailabilitySlot()
                .getStartTime()
                .isAfter(now)
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Appointment cannot be completed before "
                    + "its scheduled start time"
            );
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(now);

        Appointment savedAppointment =
            appointmentRepository.saveAndFlush(appointment);

        eventPublisher.publishEvent(
            new AppointmentCompletedEvent(
                savedAppointment.getId()
            )
        );

        return toDoctorAppointmentResponse(savedAppointment);
    }

    private User findEnabledPatient(String patientEmail) {
        return userRepository.findByEmail(patientEmail)
            .filter(User::isEnabled)
            .filter(user -> user.getRole() == Role.PATIENT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Patient account is unavailable"
            ));
    }

    private User findEnabledDoctor(String doctorEmail) {
        return userRepository.findByEmail(doctorEmail)
            .filter(User::isEnabled)
            .filter(user -> user.getRole() == Role.DOCTOR)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Doctor account is unavailable"
            ));
    }

    private AppointmentResponse toPatientAppointmentResponse(
        Appointment appointment
    ) {
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
    }

    private DoctorAppointmentResponse toDoctorAppointmentResponse(
        Appointment appointment
    ) {
        DoctorAvailabilitySlot slot =
            appointment.getAvailabilitySlot();

        User patient = appointment.getPatient();

        return new DoctorAppointmentResponse(
            appointment.getId(),
            slot.getId(),
            patient.getId(),
            patient.getFullName(),
            slot.getStartTime(),
            slot.getEndTime(),
            appointment.getConsultationFeeSnapshot(),
            appointment.getStatus(),
            appointment.getCompletedAt(),
            appointment.getCreatedAt()
        );
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