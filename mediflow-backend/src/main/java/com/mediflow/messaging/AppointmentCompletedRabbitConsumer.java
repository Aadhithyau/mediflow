package com.mediflow.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mediflow.appointment.AppointmentCompletedEvent;
import com.mediflow.config.RabbitMqConfig;
import com.mediflow.payment.AppointmentPaymentListener;

@Component
public class AppointmentCompletedRabbitConsumer {

	private final AppointmentPaymentListener paymentListener;

	public AppointmentCompletedRabbitConsumer(AppointmentPaymentListener paymentListener) {
		this.paymentListener = paymentListener;
	}

	@RabbitListener(queues = RabbitMqConfig.APPOINTMENT_COMPLETED_QUEUE)
	public void consume(AppointmentCompletedMessage message) {
		paymentListener.handleAppointmentCompleted(new AppointmentCompletedEvent(message.appointmentId()));
	}

}