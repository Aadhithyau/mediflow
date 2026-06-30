package com.mediflow.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mediflow.config.RabbitMqConfig;
import com.mediflow.notification.email.AppointmentEmailNotificationService;

@Component
public class AppointmentCancelledRabbitConsumer {

private final AppointmentEmailNotificationService
    appointmentEmailNotificationService;

public AppointmentCancelledRabbitConsumer(
    AppointmentEmailNotificationService
        appointmentEmailNotificationService
) {
    this.appointmentEmailNotificationService =
        appointmentEmailNotificationService;
}

@RabbitListener(
    queues =
        RabbitMqConfig
            .APPOINTMENT_CANCELLED_NOTIFICATION_QUEUE
)
public void consume(
    AppointmentCancelledMessage message
) {
    appointmentEmailNotificationService
        .sendCancellationEmail(
            message.appointmentId(),
            message.recipient()
        );
}

}