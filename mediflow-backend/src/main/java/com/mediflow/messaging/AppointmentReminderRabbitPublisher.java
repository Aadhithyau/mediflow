package com.mediflow.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mediflow.appointment.AppointmentReminderEvent;
import com.mediflow.config.RabbitMqConfig;

@Component
public class AppointmentReminderRabbitPublisher {

private static final Logger LOGGER =
    LoggerFactory.getLogger(
        AppointmentReminderRabbitPublisher.class
    );

private final RabbitTemplate rabbitTemplate;

public AppointmentReminderRabbitPublisher(
    RabbitTemplate rabbitTemplate
) {
    this.rabbitTemplate = rabbitTemplate;
}

@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT
)
public void publishAppointmentReminder(
    AppointmentReminderEvent event
) {
    publishForRecipient(
        event.appointmentId(),
        AppointmentEmailRecipient.PATIENT
    );

    publishForRecipient(
        event.appointmentId(),
        AppointmentEmailRecipient.DOCTOR
    );
}

private void publishForRecipient(
    Long appointmentId,
    AppointmentEmailRecipient recipient
) {
    AppointmentReminderMessage message =
        new AppointmentReminderMessage(
            appointmentId,
            recipient
        );

    try {
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
            RabbitMqConfig.APPOINTMENT_REMINDER_ROUTING_KEY,
            message
        );
    } catch (RuntimeException exception) {
        LOGGER.error(
            "Could not publish appointment-reminder "
                + "message for appointment {} "
                + "and recipient {}",
            appointmentId,
            recipient,
            exception
        );
    }
}

}