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

public static final String APPOINTMENT_BOOKED_ROUTING_KEY = "appointment.booked";

public static final String APPOINTMENT_BOOKED_NOTIFICATION_QUEUE =
		"mediflow.notification.appointment-booked";

public static final String APPOINTMENT_BOOKED_DEAD_LETTER_QUEUE =
		"mediflow.notification.appointment-booked.dlq";

public static final String APPOINTMENT_BOOKED_DEAD_LETTER_KEY =
		"notification.appointment-booked.failed";

public static final String APPOINTMENT_CANCELLED_ROUTING_KEY =
		"appointment.cancelled";

public static final String APPOINTMENT_CANCELLED_NOTIFICATION_QUEUE =
		"mediflow.notification.appointment-cancelled";

public static final String APPOINTMENT_CANCELLED_DEAD_LETTER_QUEUE =
		"mediflow.notification.appointment-cancelled.dlq";

public static final String APPOINTMENT_CANCELLED_DEAD_LETTER_KEY =
		"notification.appointment-cancelled.failed";

public static final String APPOINTMENT_REMINDER_ROUTING_KEY =
"appointment.reminder";

public static final String APPOINTMENT_REMINDER_NOTIFICATION_QUEUE =
"mediflow.notification.appointment-reminder";

public static final String APPOINTMENT_REMINDER_DEAD_LETTER_QUEUE =
"mediflow.notification.appointment-reminder.dlq";

public static final String APPOINTMENT_REMINDER_DEAD_LETTER_KEY =
"notification.appointment-reminder.failed";

public static final String DEAD_LETTER_EXCHANGE = "mediflow.dead-letter";

public static final String APPOINTMENT_COMPLETED_DEAD_LETTER_QUEUE =
		"mediflow.payment.appointment-completed.dlq";

public static final String APPOINTMENT_COMPLETED_DEAD_LETTER_KEY =
		"payment.appointment-completed.failed";

@Bean
public Declarables rabbitMqTopology() {
	DirectExchange appointmentEventsExchange =
			new DirectExchange(
					APPOINTMENT_EVENTS_EXCHANGE,
					true,
					false
			);

	DirectExchange deadLetterExchange =
			new DirectExchange(
					DEAD_LETTER_EXCHANGE,
					true,
					false
			);

	Queue appointmentCompletedQueue =
			QueueBuilder
					.durable(APPOINTMENT_COMPLETED_QUEUE)
					.deadLetterExchange(DEAD_LETTER_EXCHANGE)
					.deadLetterRoutingKey(
							APPOINTMENT_COMPLETED_DEAD_LETTER_KEY
					)
					.build();

	Queue appointmentCompletedDeadLetterQueue =
			QueueBuilder
					.durable(
							APPOINTMENT_COMPLETED_DEAD_LETTER_QUEUE
					)
					.build();

	Queue appointmentBookedNotificationQueue =
			QueueBuilder
					.durable(
							APPOINTMENT_BOOKED_NOTIFICATION_QUEUE
					)
					.deadLetterExchange(DEAD_LETTER_EXCHANGE)
					.deadLetterRoutingKey(
							APPOINTMENT_BOOKED_DEAD_LETTER_KEY
					)
					.build();

	Queue appointmentBookedDeadLetterQueue =
			QueueBuilder
					.durable(
							APPOINTMENT_BOOKED_DEAD_LETTER_QUEUE
					)
					.build();

	Queue appointmentCancelledNotificationQueue =
			QueueBuilder
					.durable(
							APPOINTMENT_CANCELLED_NOTIFICATION_QUEUE
					)
					.deadLetterExchange(DEAD_LETTER_EXCHANGE)
					.deadLetterRoutingKey(
							APPOINTMENT_CANCELLED_DEAD_LETTER_KEY
					)
					.build();

	Queue appointmentCancelledDeadLetterQueue =
			QueueBuilder
					.durable(
							APPOINTMENT_CANCELLED_DEAD_LETTER_QUEUE
					)
					.build();
	Queue appointmentReminderNotificationQueue = QueueBuilder.durable(APPOINTMENT_REMINDER_NOTIFICATION_QUEUE)
			.deadLetterExchange(DEAD_LETTER_EXCHANGE).deadLetterRoutingKey(APPOINTMENT_REMINDER_DEAD_LETTER_KEY)
			.build();

	Queue appointmentReminderDeadLetterQueue = QueueBuilder.durable(APPOINTMENT_REMINDER_DEAD_LETTER_QUEUE).build();

	Binding appointmentCompletedBinding =
			BindingBuilder
					.bind(appointmentCompletedQueue)
					.to(appointmentEventsExchange)
					.with(
							APPOINTMENT_COMPLETED_ROUTING_KEY
					);

	Binding appointmentBookedBinding =
			BindingBuilder
					.bind(appointmentBookedNotificationQueue)
					.to(appointmentEventsExchange)
					.with(
							APPOINTMENT_BOOKED_ROUTING_KEY
					);

	Binding appointmentCancelledBinding =
			BindingBuilder
					.bind(
							appointmentCancelledNotificationQueue
					)
					.to(appointmentEventsExchange)
					.with(
							APPOINTMENT_CANCELLED_ROUTING_KEY
					);
	Binding appointmentReminderBinding = BindingBuilder.bind(appointmentReminderNotificationQueue)
			.to(appointmentEventsExchange).with(APPOINTMENT_REMINDER_ROUTING_KEY);

	Binding deadLetterBinding =
			BindingBuilder
					.bind(
							appointmentCompletedDeadLetterQueue
					)
					.to(deadLetterExchange)
					.with(
							APPOINTMENT_COMPLETED_DEAD_LETTER_KEY
					);

	Binding appointmentBookedDeadLetterBinding =
			BindingBuilder
					.bind(
							appointmentBookedDeadLetterQueue
					)
					.to(deadLetterExchange)
					.with(
							APPOINTMENT_BOOKED_DEAD_LETTER_KEY
					);

	Binding appointmentCancelledDeadLetterBinding =
			BindingBuilder
					.bind(
							appointmentCancelledDeadLetterQueue
					)
					.to(deadLetterExchange)
					.with(
							APPOINTMENT_CANCELLED_DEAD_LETTER_KEY
					);
	Binding appointmentReminderDeadLetterBinding = BindingBuilder.bind(appointmentReminderDeadLetterQueue)
			.to(deadLetterExchange).with(APPOINTMENT_REMINDER_DEAD_LETTER_KEY);

	return new Declarables(
			appointmentEventsExchange,
			deadLetterExchange,
			appointmentCompletedQueue,
			appointmentCompletedDeadLetterQueue,
			appointmentBookedNotificationQueue,
			appointmentBookedDeadLetterQueue,
			appointmentCancelledNotificationQueue,
			appointmentCancelledDeadLetterQueue,
			appointmentCompletedBinding,
			appointmentBookedBinding,
			appointmentCancelledBinding,
			deadLetterBinding,
			appointmentBookedDeadLetterBinding,
			appointmentCancelledDeadLetterBinding,
			appointmentReminderNotificationQueue,
			appointmentReminderDeadLetterQueue,
			appointmentReminderBinding,
			appointmentReminderDeadLetterBinding
	);
}

@Bean
public MessageConverter rabbitMessageConverter() {
	return new JacksonJsonMessageConverter(
			"com.mediflow.messaging"
	);
}

}