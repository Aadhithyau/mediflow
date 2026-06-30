package com.mediflow.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mediflow.appointment.AppointmentBookedEvent;
import com.mediflow.config.RabbitMqConfig;

@Component
public class AppointmentBookedRabbitPublisher {

private static final Logger LOGGER =
    LoggerFactory.getLogger(
        AppointmentBookedRabbitPublisher.class
    );

private final RabbitTemplate rabbitTemplate;

public AppointmentBookedRabbitPublisher(
    RabbitTemplate rabbitTemplate
) {
    this.rabbitTemplate = rabbitTemplate;
}

@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT
)
public void publishAppointmentBooked(
    AppointmentBookedEvent event
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
    AppointmentBookedMessage message =
        new AppointmentBookedMessage(
            appointmentId,
            recipient
        );

    try {
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
            RabbitMqConfig.APPOINTMENT_BOOKED_ROUTING_KEY,
            message
        );
    } catch (RuntimeException exception) {
        LOGGER.error(
            "Could not publish appointment-booked "
                + "message for appointment {} "
                + "and recipient {}",
            appointmentId,
            recipient,
            exception
        );
    }
}

}