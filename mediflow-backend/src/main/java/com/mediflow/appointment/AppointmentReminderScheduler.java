package com.mediflow.appointment;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentReminderScheduler {

private final AppointmentRepository appointmentRepository;
private final ApplicationEventPublisher eventPublisher;

public AppointmentReminderScheduler(
    AppointmentRepository appointmentRepository,
    ApplicationEventPublisher eventPublisher
) {
    this.appointmentRepository = appointmentRepository;
    this.eventPublisher = eventPublisher;
}

@Scheduled(
    fixedDelayString =
        "${app.reminder.scan-delay-ms:300000}"
)
@Transactional
public void queueDueReminders() {
    OffsetDateTime now = OffsetDateTime.now();

    List<Appointment> dueAppointments =
        appointmentRepository.findDueForReminder(
            AppointmentStatus.BOOKED,
            now,
            now.plusHours(24)
        );

    for (Appointment appointment : dueAppointments) {
        appointment.setReminderQueuedAt(now);

        eventPublisher.publishEvent(
            new AppointmentReminderEvent(
                appointment.getId()
            )
        );
    }
}

}