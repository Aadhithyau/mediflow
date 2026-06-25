package com.mediflow.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mediflow.appointment.AppointmentCompletedEvent;
import com.mediflow.config.RabbitMqConfig;

@Component
public class AppointmentCompletedRabbitPublisher {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentCompletedRabbitPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public AppointmentCompletedRabbitPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publishAppointmentCompleted(AppointmentCompletedEvent event) {
		AppointmentCompletedMessage message = new AppointmentCompletedMessage(event.appointmentId());

		try {
			rabbitTemplate.convertAndSend(RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
					RabbitMqConfig.APPOINTMENT_COMPLETED_ROUTING_KEY, message);
		} catch (RuntimeException exception) {
			LOGGER.error("Could not publish appointment-completed " + "message for appointment {}",
					event.appointmentId(), exception);
		}
	}

}
