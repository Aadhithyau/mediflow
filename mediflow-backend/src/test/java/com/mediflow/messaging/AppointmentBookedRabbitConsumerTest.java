package com.mediflow.messaging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mediflow.notification.email.AppointmentEmailNotificationService;

@ExtendWith(MockitoExtension.class)
class AppointmentBookedRabbitConsumerTest {

@Mock
private AppointmentEmailNotificationService
    appointmentEmailNotificationService;

@Test
void consumerDelegatesPatientBookingEmailProcessing() {
    AppointmentBookedRabbitConsumer consumer =
        new AppointmentBookedRabbitConsumer(
            appointmentEmailNotificationService
        );

    consumer.consume(
        new AppointmentBookedMessage(
            125L,
            AppointmentEmailRecipient.PATIENT
        )
    );

    verify(appointmentEmailNotificationService)
        .sendBookingEmail(
            125L,
            AppointmentEmailRecipient.PATIENT
        );
}

}