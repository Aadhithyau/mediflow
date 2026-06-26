package com.mediflow.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mediflow.config.RabbitMqConfig;
import com.mediflow.payment.AppointmentPaymentProcessor;

@Component
public class AppointmentCompletedRabbitConsumer {

private final AppointmentPaymentProcessor paymentProcessor;

public AppointmentCompletedRabbitConsumer(
    AppointmentPaymentProcessor paymentProcessor
) {
    this.paymentProcessor = paymentProcessor;
}

@RabbitListener(
    queues = RabbitMqConfig.APPOINTMENT_COMPLETED_QUEUE
)
public void consume(
    AppointmentCompletedMessage message
) {
    paymentProcessor.process(
        message.appointmentId()
    );
}

}