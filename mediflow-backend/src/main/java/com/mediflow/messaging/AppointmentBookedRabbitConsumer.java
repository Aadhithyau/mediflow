package com.mediflow.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mediflow.config.RabbitMqConfig;
import com.mediflow.notification.email.AppointmentEmailNotificationService;

@Component
public class AppointmentBookedRabbitConsumer {

private final AppointmentEmailNotificationService
    appointmentEmailNotificationService;

public AppointmentBookedRabbitConsumer(
    AppointmentEmailNotificationService
        appointmentEmailNotificationService
) {
    this.appointmentEmailNotificationService =
        appointmentEmailNotificationService;
}

@RabbitListener(
    queues =
        RabbitMqConfig
            .APPOINTMENT_BOOKED_NOTIFICATION_QUEUE
)
public void consume(
    AppointmentBookedMessage message
) {
    appointmentEmailNotificationService
        .sendBookingEmail(
            message.appointmentId(),
            message.recipient()
        );
}

}