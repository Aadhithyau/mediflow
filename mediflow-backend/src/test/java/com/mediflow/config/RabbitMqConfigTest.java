package com.mediflow.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

class RabbitMqConfigTest {

	private final RabbitMqConfig config = new RabbitMqConfig();

	@Test
	void topologyDeclaresDurableQueuesAndDeadLetterRouting() {
		Declarables topology = config.rabbitMqTopology();

		List<DirectExchange> exchanges = topology.getDeclarablesByType(DirectExchange.class);

		assertThat(exchanges).extracting(DirectExchange::getName).containsExactlyInAnyOrder(
				RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE, RabbitMqConfig.DEAD_LETTER_EXCHANGE);

		assertThat(exchanges).allSatisfy(exchange -> {
			assertThat(exchange.isDurable()).isTrue();
			assertThat(exchange.isAutoDelete()).isFalse();
		});

		List<Queue> queues = topology.getDeclarablesByType(Queue.class);

		Queue appointmentQueue = queues.stream()
				.filter(queue -> queue.getName().equals(RabbitMqConfig.APPOINTMENT_COMPLETED_QUEUE)).findFirst()
				.orElseThrow();

		Queue deadLetterQueue = queues.stream()
				.filter(queue -> queue.getName().equals(RabbitMqConfig.APPOINTMENT_COMPLETED_DEAD_LETTER_QUEUE))
				.findFirst().orElseThrow();

		assertThat(appointmentQueue.isDurable()).isTrue();
		assertThat(appointmentQueue.isExclusive()).isFalse();
		assertThat(appointmentQueue.isAutoDelete()).isFalse();

		assertThat(appointmentQueue.getArguments())
				.containsEntry("x-dead-letter-exchange", RabbitMqConfig.DEAD_LETTER_EXCHANGE)
				.containsEntry("x-dead-letter-routing-key", RabbitMqConfig.APPOINTMENT_COMPLETED_DEAD_LETTER_KEY);

		assertThat(deadLetterQueue.isDurable()).isTrue();
		assertThat(deadLetterQueue.isExclusive()).isFalse();
		assertThat(deadLetterQueue.isAutoDelete()).isFalse();

		List<Binding> bindings = topology.getDeclarablesByType(Binding.class);

		assertThat(bindings).anySatisfy(binding -> {
			assertThat(binding.getExchange()).isEqualTo(RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE);

			assertThat(binding.getDestination()).isEqualTo(RabbitMqConfig.APPOINTMENT_COMPLETED_QUEUE);

			assertThat(binding.getRoutingKey()).isEqualTo(RabbitMqConfig.APPOINTMENT_COMPLETED_ROUTING_KEY);
		});

		assertThat(bindings).anySatisfy(binding -> {
			assertThat(binding.getExchange()).isEqualTo(RabbitMqConfig.DEAD_LETTER_EXCHANGE);

			assertThat(binding.getDestination()).isEqualTo(RabbitMqConfig.APPOINTMENT_COMPLETED_DEAD_LETTER_QUEUE);

			assertThat(binding.getRoutingKey()).isEqualTo(RabbitMqConfig.APPOINTMENT_COMPLETED_DEAD_LETTER_KEY);
		});
	}

	@Test
	void rabbitMessagesUseJacksonJsonConversion() {
		assertThat(config.rabbitMessageConverter()).isInstanceOf(JacksonJsonMessageConverter.class);
	}

}
