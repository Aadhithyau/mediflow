package com.mediflow.messaging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.mediflow.appointment.AppointmentBookedEvent;
import com.mediflow.config.RabbitMqConfig;

@ExtendWith(MockitoExtension.class)
class AppointmentBookedRabbitPublisherTest {

@Mock
private RabbitTemplate rabbitTemplate;

@Test
void publishesSeparatePatientAndDoctorMessages() {
    AppointmentBookedRabbitPublisher publisher =
        new AppointmentBookedRabbitPublisher(
            rabbitTemplate
        );

    publisher.publishAppointmentBooked(
        new AppointmentBookedEvent(125L)
    );

    verify(rabbitTemplate).convertAndSend(
        RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
        RabbitMqConfig.APPOINTMENT_BOOKED_ROUTING_KEY,
        new AppointmentBookedMessage(
            125L,
            AppointmentEmailRecipient.PATIENT
        )
    );

    verify(rabbitTemplate).convertAndSend(
        RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
        RabbitMqConfig.APPOINTMENT_BOOKED_ROUTING_KEY,
        new AppointmentBookedMessage(
            125L,
            AppointmentEmailRecipient.DOCTOR
        )
    );
}

}