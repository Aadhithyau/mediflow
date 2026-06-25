package com.mediflow.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RabbitMqConfig {

	public static final String APPOINTMENT_EVENTS_EXCHANGE = "mediflow.appointment.events";

	public static final String APPOINTMENT_COMPLETED_QUEUE = "mediflow.payment.appointment-completed";

	public static final String APPOINTMENT_COMPLETED_ROUTING_KEY = "appointment.completed";

	public static final String DEAD_LETTER_EXCHANGE = "mediflow.dead-letter";

	public static final String APPOINTMENT_COMPLETED_DEAD_LETTER_QUEUE = "mediflow.payment.appointment-completed.dlq";

	public static final String APPOINTMENT_COMPLETED_DEAD_LETTER_KEY = "payment.appointment-completed.failed";

	@Bean
	public Declarables rabbitMqTopology() {
		DirectExchange appointmentEventsExchange = new DirectExchange(APPOINTMENT_EVENTS_EXCHANGE, true, false);

		DirectExchange deadLetterExchange = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);

		Queue appointmentCompletedQueue = QueueBuilder.durable(APPOINTMENT_COMPLETED_QUEUE)
				.deadLetterExchange(DEAD_LETTER_EXCHANGE).deadLetterRoutingKey(APPOINTMENT_COMPLETED_DEAD_LETTER_KEY)
				.build();

		Queue appointmentCompletedDeadLetterQueue = QueueBuilder.durable(APPOINTMENT_COMPLETED_DEAD_LETTER_QUEUE)
				.build();

		Binding appointmentCompletedBinding = BindingBuilder.bind(appointmentCompletedQueue)
				.to(appointmentEventsExchange).with(APPOINTMENT_COMPLETED_ROUTING_KEY);

		Binding deadLetterBinding = BindingBuilder.bind(appointmentCompletedDeadLetterQueue).to(deadLetterExchange)
				.with(APPOINTMENT_COMPLETED_DEAD_LETTER_KEY);

		return new Declarables(appointmentEventsExchange, deadLetterExchange, appointmentCompletedQueue,
				appointmentCompletedDeadLetterQueue, appointmentCompletedBinding, deadLetterBinding);
	}

	@Bean
	public MessageConverter rabbitMessageConverter() {
		return new JacksonJsonMessageConverter("com.mediflow.messaging");
	}

}
