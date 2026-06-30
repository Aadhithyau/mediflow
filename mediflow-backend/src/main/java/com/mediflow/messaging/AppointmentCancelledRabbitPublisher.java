package com.mediflow.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mediflow.appointment.AppointmentCancelledEvent;
import com.mediflow.config.RabbitMqConfig;

@Component
public class AppointmentCancelledRabbitPublisher {

private static final Logger LOGGER =
    LoggerFactory.getLogger(
        AppointmentCancelledRabbitPublisher.class
    );

private final RabbitTemplate rabbitTemplate;

public AppointmentCancelledRabbitPublisher(
    RabbitTemplate rabbitTemplate
) {
    this.rabbitTemplate = rabbitTemplate;
}

@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT
)
public void publishAppointmentCancelled(
    AppointmentCancelledEvent event
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
    AppointmentCancelledMessage message =
        new AppointmentCancelledMessage(
            appointmentId,
            recipient
        );

    try {
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
            RabbitMqConfig.APPOINTMENT_CANCELLED_ROUTING_KEY,
            message
        );
    } catch (RuntimeException exception) {
        LOGGER.error(
            "Could not publish appointment-cancelled "
                + "message for appointment {} "
                + "and recipient {}",
            appointmentId,
            recipient,
            exception
        );
    }
}

}