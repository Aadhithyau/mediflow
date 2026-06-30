package com.mediflow.messaging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mediflow.notification.email.AppointmentEmailNotificationService;

@ExtendWith(MockitoExtension.class)
class AppointmentCancelledRabbitConsumerTest {

@Mock
private AppointmentEmailNotificationService
    appointmentEmailNotificationService;

@Test
void consumerDelegatesCancellationEmailProcessing() {
    AppointmentCancelledRabbitConsumer consumer =
        new AppointmentCancelledRabbitConsumer(
            appointmentEmailNotificationService
        );

    consumer.consume(
        new AppointmentCancelledMessage(
            125L,
            AppointmentEmailRecipient.PATIENT
        )
    );

    verify(appointmentEmailNotificationService)
        .sendCancellationEmail(
            125L,
            AppointmentEmailRecipient.PATIENT
        );
}

}