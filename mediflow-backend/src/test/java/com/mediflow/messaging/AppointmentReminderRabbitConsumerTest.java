package com.mediflow.messaging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mediflow.notification.email.AppointmentEmailNotificationService;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderRabbitConsumerTest {

@Mock
private AppointmentEmailNotificationService
    appointmentEmailNotificationService;

@Test
void consumerDelegatesReminderEmailProcessing() {
    AppointmentReminderRabbitConsumer consumer =
        new AppointmentReminderRabbitConsumer(
            appointmentEmailNotificationService
        );

    consumer.consume(
        new AppointmentReminderMessage(
            125L,
            AppointmentEmailRecipient.PATIENT
        )
    );

    verify(appointmentEmailNotificationService)
        .sendReminderEmail(
            125L,
            AppointmentEmailRecipient.PATIENT
        );
}

}