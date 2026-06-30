package com.mediflow.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mediflow.config.RabbitMqConfig;
import com.mediflow.notification.email.AppointmentEmailNotificationService;

@Component
public class AppointmentReminderRabbitConsumer {

private final AppointmentEmailNotificationService
    appointmentEmailNotificationService;

public AppointmentReminderRabbitConsumer(
    AppointmentEmailNotificationService
        appointmentEmailNotificationService
) {
    this.appointmentEmailNotificationService =
        appointmentEmailNotificationService;
}

@RabbitListener(
    queues =
        RabbitMqConfig
            .APPOINTMENT_REMINDER_NOTIFICATION_QUEUE
)
public void consume(
    AppointmentReminderMessage message
) {
    appointmentEmailNotificationService
        .sendReminderEmail(
            message.appointmentId(),
            message.recipient()
        );
}

}