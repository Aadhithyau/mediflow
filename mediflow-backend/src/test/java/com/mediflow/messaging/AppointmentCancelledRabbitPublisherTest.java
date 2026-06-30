package com.mediflow.messaging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.mediflow.appointment.AppointmentCancelledEvent;
import com.mediflow.config.RabbitMqConfig;

@ExtendWith(MockitoExtension.class)
class AppointmentCancelledRabbitPublisherTest {

@Mock
private RabbitTemplate rabbitTemplate;

@Test
void publishesSeparatePatientAndDoctorMessages() {
    AppointmentCancelledRabbitPublisher publisher =
        new AppointmentCancelledRabbitPublisher(
            rabbitTemplate
        );

    publisher.publishAppointmentCancelled(
        new AppointmentCancelledEvent(125L)
    );

    verify(rabbitTemplate).convertAndSend(
        RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
        RabbitMqConfig.APPOINTMENT_CANCELLED_ROUTING_KEY,
        new AppointmentCancelledMessage(
            125L,
            AppointmentEmailRecipient.PATIENT
        )
    );

    verify(rabbitTemplate).convertAndSend(
        RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
        RabbitMqConfig.APPOINTMENT_CANCELLED_ROUTING_KEY,
        new AppointmentCancelledMessage(
            125L,
            AppointmentEmailRecipient.DOCTOR
        )
    );
}

}