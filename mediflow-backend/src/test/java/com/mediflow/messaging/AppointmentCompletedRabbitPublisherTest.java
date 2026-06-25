package com.mediflow.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.mediflow.appointment.AppointmentCompletedEvent;
import com.mediflow.config.RabbitMqConfig;

@ExtendWith(MockitoExtension.class)
class AppointmentCompletedRabbitPublisherTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@InjectMocks
	private AppointmentCompletedRabbitPublisher publisher;

	@Test
	void publishesAppointmentCompletedMessage() {
		AppointmentCompletedEvent event = new AppointmentCompletedEvent(42L);

		publisher.publishAppointmentCompleted(event);

		verify(rabbitTemplate).convertAndSend(RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
				RabbitMqConfig.APPOINTMENT_COMPLETED_ROUTING_KEY, new AppointmentCompletedMessage(42L));
	}

	@Test
	void rabbitFailureDoesNotEscapePublisher() {
		AppointmentCompletedEvent event = new AppointmentCompletedEvent(43L);

		AppointmentCompletedMessage message = new AppointmentCompletedMessage(43L);

		doThrow(new AmqpException("RabbitMQ unavailable")).when(rabbitTemplate).convertAndSend(
				RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE, RabbitMqConfig.APPOINTMENT_COMPLETED_ROUTING_KEY, message);

		assertThatCode(() -> publisher.publishAppointmentCompleted(event)).doesNotThrowAnyException();

		verify(rabbitTemplate).convertAndSend(RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
				RabbitMqConfig.APPOINTMENT_COMPLETED_ROUTING_KEY, message);
	}

}
