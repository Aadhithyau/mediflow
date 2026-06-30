package com.mediflow.appointment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderSchedulerTest {

@Mock
private AppointmentRepository appointmentRepository;

@Mock
private ApplicationEventPublisher eventPublisher;

@Mock
private Appointment appointment;

@Test
void queuesReminderAndMarksAppointment() {
    when(
        appointmentRepository.findDueForReminder(
            eq(AppointmentStatus.BOOKED),
            any(OffsetDateTime.class),
            any(OffsetDateTime.class)
        )
    ).thenReturn(List.of(appointment));

    when(appointment.getId()).thenReturn(125L);

    AppointmentReminderScheduler scheduler =
        new AppointmentReminderScheduler(
            appointmentRepository,
            eventPublisher
        );

    scheduler.queueDueReminders();

    verify(appointment).setReminderQueuedAt(
        any(OffsetDateTime.class)
    );

    verify(eventPublisher).publishEvent(
        new AppointmentReminderEvent(125L)
    );
}

}