package com.mediflow.messaging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.mediflow.appointment.AppointmentReminderEvent;
import com.mediflow.config.RabbitMqConfig;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderRabbitPublisherTest {

@Mock
private RabbitTemplate rabbitTemplate;

@Test
void publishesSeparatePatientAndDoctorMessages() {
    AppointmentReminderRabbitPublisher publisher =
        new AppointmentReminderRabbitPublisher(
            rabbitTemplate
        );

    publisher.publishAppointmentReminder(
        new AppointmentReminderEvent(125L)
    );

    verify(rabbitTemplate).convertAndSend(
        RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
        RabbitMqConfig.APPOINTMENT_REMINDER_ROUTING_KEY,
        new AppointmentReminderMessage(
            125L,
            AppointmentEmailRecipient.PATIENT
        )
    );

    verify(rabbitTemplate).convertAndSend(
        RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
        RabbitMqConfig.APPOINTMENT_REMINDER_ROUTING_KEY,
        new AppointmentReminderMessage(
            125L,
            AppointmentEmailRecipient.DOCTOR
        )
    );
}

}