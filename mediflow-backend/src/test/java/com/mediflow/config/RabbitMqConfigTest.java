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

private final RabbitMqConfig config =
    new RabbitMqConfig();

@Test
void topologyDeclaresAllQueuesAndBindings() {
    Declarables topology =
        config.rabbitMqTopology();

    List<DirectExchange> exchanges =
        topology.getDeclarablesByType(
            DirectExchange.class
        );

    assertThat(exchanges)
        .extracting(DirectExchange::getName)
        .containsExactlyInAnyOrder(
            RabbitMqConfig
                .APPOINTMENT_EVENTS_EXCHANGE,
            RabbitMqConfig.DEAD_LETTER_EXCHANGE
        );

    assertThat(exchanges).allSatisfy(exchange -> {
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    });

    List<Queue> queues =
        topology.getDeclarablesByType(Queue.class);

    assertMainQueue(
        queues,
        RabbitMqConfig.APPOINTMENT_COMPLETED_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_COMPLETED_DEAD_LETTER_KEY
    );

    assertDeadLetterQueue(
        queues,
        RabbitMqConfig
            .APPOINTMENT_COMPLETED_DEAD_LETTER_QUEUE
    );

    assertMainQueue(
        queues,
        RabbitMqConfig
            .APPOINTMENT_BOOKED_NOTIFICATION_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_BOOKED_DEAD_LETTER_KEY
    );

    assertDeadLetterQueue(
        queues,
        RabbitMqConfig
            .APPOINTMENT_BOOKED_DEAD_LETTER_QUEUE
    );

    assertMainQueue(
        queues,
        RabbitMqConfig
            .APPOINTMENT_CANCELLED_NOTIFICATION_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_CANCELLED_DEAD_LETTER_KEY
    );

    assertDeadLetterQueue(
        queues,
        RabbitMqConfig
            .APPOINTMENT_CANCELLED_DEAD_LETTER_QUEUE
    );

    assertMainQueue(
        queues,
        RabbitMqConfig
            .APPOINTMENT_REMINDER_NOTIFICATION_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_REMINDER_DEAD_LETTER_KEY
    );

    assertDeadLetterQueue(
        queues,
        RabbitMqConfig
            .APPOINTMENT_REMINDER_DEAD_LETTER_QUEUE
    );

    List<Binding> bindings =
        topology.getDeclarablesByType(
            Binding.class
        );

    assertBinding(
        bindings,
        RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
        RabbitMqConfig.APPOINTMENT_COMPLETED_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_COMPLETED_ROUTING_KEY
    );

    assertBinding(
        bindings,
        RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
        RabbitMqConfig
            .APPOINTMENT_BOOKED_NOTIFICATION_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_BOOKED_ROUTING_KEY
    );

    assertBinding(
        bindings,
        RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
        RabbitMqConfig
            .APPOINTMENT_CANCELLED_NOTIFICATION_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_CANCELLED_ROUTING_KEY
    );

    assertBinding(
        bindings,
        RabbitMqConfig.APPOINTMENT_EVENTS_EXCHANGE,
        RabbitMqConfig
            .APPOINTMENT_REMINDER_NOTIFICATION_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_REMINDER_ROUTING_KEY
    );

    assertBinding(
        bindings,
        RabbitMqConfig.DEAD_LETTER_EXCHANGE,
        RabbitMqConfig
            .APPOINTMENT_COMPLETED_DEAD_LETTER_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_COMPLETED_DEAD_LETTER_KEY
    );

    assertBinding(
        bindings,
        RabbitMqConfig.DEAD_LETTER_EXCHANGE,
        RabbitMqConfig
            .APPOINTMENT_BOOKED_DEAD_LETTER_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_BOOKED_DEAD_LETTER_KEY
    );

    assertBinding(
        bindings,
        RabbitMqConfig.DEAD_LETTER_EXCHANGE,
        RabbitMqConfig
            .APPOINTMENT_CANCELLED_DEAD_LETTER_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_CANCELLED_DEAD_LETTER_KEY
    );

    assertBinding(
        bindings,
        RabbitMqConfig.DEAD_LETTER_EXCHANGE,
        RabbitMqConfig
            .APPOINTMENT_REMINDER_DEAD_LETTER_QUEUE,
        RabbitMqConfig
            .APPOINTMENT_REMINDER_DEAD_LETTER_KEY
    );
}

@Test
void rabbitMessagesUseJacksonJsonConversion() {
    assertThat(config.rabbitMessageConverter())
        .isInstanceOf(
            JacksonJsonMessageConverter.class
        );
}

private void assertMainQueue(
    List<Queue> queues,
    String queueName,
    String deadLetterRoutingKey
) {
    Queue queue = findQueue(queues, queueName);

    assertThat(queue.isDurable()).isTrue();
    assertThat(queue.isExclusive()).isFalse();
    assertThat(queue.isAutoDelete()).isFalse();

    assertThat(queue.getArguments())
        .containsEntry(
            "x-dead-letter-exchange",
            RabbitMqConfig.DEAD_LETTER_EXCHANGE
        )
        .containsEntry(
            "x-dead-letter-routing-key",
            deadLetterRoutingKey
        );
}

private void assertDeadLetterQueue(
    List<Queue> queues,
    String queueName
) {
    Queue queue = findQueue(queues, queueName);

    assertThat(queue.isDurable()).isTrue();
    assertThat(queue.isExclusive()).isFalse();
    assertThat(queue.isAutoDelete()).isFalse();
}

private Queue findQueue(
    List<Queue> queues,
    String queueName
) {
    return queues.stream()
        .filter(queue ->
            queue.getName().equals(queueName)
        )
        .findFirst()
        .orElseThrow();
}

private void assertBinding(
    List<Binding> bindings,
    String exchange,
    String destination,
    String routingKey
) {
    assertThat(bindings).anySatisfy(binding -> {
        assertThat(binding.getExchange())
            .isEqualTo(exchange);

        assertThat(binding.getDestination())
            .isEqualTo(destination);

        assertThat(binding.getRoutingKey())
            .isEqualTo(routingKey);
    });
}

}