package com.mediflow.notification.email;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediflow.appointment.Appointment;
import com.mediflow.appointment.AppointmentRepository;
import com.mediflow.availability.DoctorAvailabilitySlot;
import com.mediflow.doctor.DoctorProfile;
import com.mediflow.messaging.AppointmentEmailRecipient;
import com.mediflow.user.User;

@Service
public class AppointmentEmailNotificationService {

private static final DateTimeFormatter DATE_TIME_FORMATTER =
    DateTimeFormatter.ofPattern(
        "dd MMMM yyyy, hh:mm a"
    );

private final AppointmentRepository appointmentRepository;
private final EmailSender emailSender;

public AppointmentEmailNotificationService(
    AppointmentRepository appointmentRepository,
    EmailSender emailSender
) {
    this.appointmentRepository = appointmentRepository;
    this.emailSender = emailSender;
}

@Transactional(readOnly = true)
public void sendBookingEmail(
    Long appointmentId,
    AppointmentEmailRecipient recipient
) {
    AppointmentEmailDetails details =
        loadAppointmentDetails(appointmentId);

    switch (recipient) {
        case PATIENT -> emailSender.send(
            details.patientEmail(),
            "MediFlow appointment confirmed",
            patientBookingBody(details)
        );

        case DOCTOR -> emailSender.send(
            details.doctorEmail(),
            "New MediFlow appointment booked",
            doctorBookingBody(details)
        );
    }
}

@Transactional(readOnly = true)
public void sendCancellationEmail(
    Long appointmentId,
    AppointmentEmailRecipient recipient
) {
    AppointmentEmailDetails details =
        loadAppointmentDetails(appointmentId);

    switch (recipient) {
        case PATIENT -> emailSender.send(
            details.patientEmail(),
            "MediFlow appointment cancelled",
            patientCancellationBody(details)
        );

        case DOCTOR -> emailSender.send(
            details.doctorEmail(),
            "MediFlow appointment cancelled",
            doctorCancellationBody(details)
        );
    }
}

@Transactional(readOnly = true)
public void sendReminderEmail(
    Long appointmentId,
    AppointmentEmailRecipient recipient
) {
    AppointmentEmailDetails details =
        loadAppointmentDetails(appointmentId);

    switch (recipient) {
        case PATIENT -> emailSender.send(
            details.patientEmail(),
            "Reminder: Your MediFlow appointment is coming up",
            patientReminderBody(details)
        );

        case DOCTOR -> emailSender.send(
            details.doctorEmail(),
            "Reminder: You have an upcoming MediFlow appointment",
            doctorReminderBody(details)
        );
    }
}

private AppointmentEmailDetails loadAppointmentDetails(
    Long appointmentId
) {
    Appointment appointment = appointmentRepository
        .findById(appointmentId)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "Appointment was not found: "
                    + appointmentId
            )
        );

    DoctorAvailabilitySlot slot =
        appointment.getAvailabilitySlot();

    DoctorProfile doctorProfile =
        slot.getDoctorProfile();

    User patient = appointment.getPatient();
    User doctor = doctorProfile.getUser();

    return new AppointmentEmailDetails(
        appointment.getId(),
        patient.getFullName(),
        patient.getEmail(),
        doctor.getFullName(),
        doctor.getEmail(),
        slot.getStartTime(),
        slot.getEndTime(),
        doctorProfile.getHospitalName(),
        doctorProfile.getHospitalAddress()
    );
}

private String patientReminderBody(
	    AppointmentEmailDetails details
	) {
	    return """
	        Hello %s,

	        Just a friendly reminder that your appointment with %s is coming up within the next 24 hours.

	        Date and time: %s to %s
	        Hospital: %s
	        Address: %s
	        Appointment ID: %d

	        Please arrive a few minutes early. You may also bring any previous medical reports, prescriptions, or test results that could help during the consultation.

	        We hope your appointment goes smoothly.

	        Regards,
	        MediFlow
	        """.formatted(
	            details.patientName(),
	            details.doctorName(),
	            format(details.startTime()),
	            format(details.endTime()),
	            valueOrUnavailable(
	                details.hospitalName()
	            ),
	            valueOrUnavailable(
	                details.hospitalAddress()
	            ),
	            details.appointmentId()
	        );
	}

	private String doctorReminderBody(
	    AppointmentEmailDetails details
	) {
	    return """
	        Hello Dr. %s,

	        This is a quick reminder about your appointment scheduled within the next 24 hours.

	        Patient: %s
	        Date and time: %s to %s
	        Hospital: %s
	        Address: %s
	        Appointment ID: %d

	        Please review the appointment details before the consultation.

	        Regards,
	        MediFlow
	        """.formatted(
	            details.doctorName(),
	            details.patientName(),
	            format(details.startTime()),
	            format(details.endTime()),
	            valueOrUnavailable(
	                details.hospitalName()
	            ),
	            valueOrUnavailable(
	                details.hospitalAddress()
	            ),
	            details.appointmentId()
	        );
	}
	
private String patientBookingBody(
    AppointmentEmailDetails details
) {
    return """
        Hello %s,

        Your appointment has been confirmed.

        Doctor: %s
        Date and time: %s to %s
        Hospital: %s
        Address: %s
        Appointment ID: %d

        Please arrive a few minutes before the scheduled time.

        Regards,
        MediFlow
        """.formatted(
            details.patientName(),
            details.doctorName(),
            format(details.startTime()),
            format(details.endTime()),
            valueOrUnavailable(
                details.hospitalName()
            ),
            valueOrUnavailable(
                details.hospitalAddress()
            ),
            details.appointmentId()
        );
}

private String doctorBookingBody(
    AppointmentEmailDetails details
) {
    return """
        Hello %s,

        A new appointment has been booked.

        Patient: %s
        Date and time: %s to %s
        Hospital: %s
        Address: %s
        Appointment ID: %d

        Regards,
        MediFlow
        """.formatted(
            details.doctorName(),
            details.patientName(),
            format(details.startTime()),
            format(details.endTime()),
            valueOrUnavailable(
                details.hospitalName()
            ),
            valueOrUnavailable(
                details.hospitalAddress()
            ),
            details.appointmentId()
        );
}

private String patientCancellationBody(
    AppointmentEmailDetails details
) {
    return """
        Hello %s,

        Your appointment has been cancelled.

        Doctor: %s
        Scheduled time: %s to %s
        Hospital: %s
        Appointment ID: %d

        Regards,
        MediFlow
        """.formatted(
            details.patientName(),
            details.doctorName(),
            format(details.startTime()),
            format(details.endTime()),
            valueOrUnavailable(
                details.hospitalName()
            ),
            details.appointmentId()
        );
}

private String doctorCancellationBody(
    AppointmentEmailDetails details
) {
    return """
        Hello %s,

        The following appointment has been cancelled.

        Patient: %s
        Scheduled time: %s to %s
        Hospital: %s
        Appointment ID: %d

        Regards,
        MediFlow
        """.formatted(
            details.doctorName(),
            details.patientName(),
            format(details.startTime()),
            format(details.endTime()),
            valueOrUnavailable(
                details.hospitalName()
            ),
            details.appointmentId()
        );
}

private String format(
    java.time.OffsetDateTime dateTime
) {
    return DATE_TIME_FORMATTER.format(dateTime);
}

private String valueOrUnavailable(String value) {
    return value == null || value.isBlank()
        ? "Not provided"
        : value;
}

}