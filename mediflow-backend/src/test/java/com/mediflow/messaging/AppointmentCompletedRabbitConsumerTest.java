package com.mediflow.messaging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mediflow.payment.AppointmentPaymentProcessor;

@ExtendWith(MockitoExtension.class)
class AppointmentCompletedRabbitConsumerTest {

@Mock
private AppointmentPaymentProcessor paymentProcessor;

@InjectMocks
private AppointmentCompletedRabbitConsumer consumer;

@Test
void delegatesMessageToPaymentProcessor() {
    AppointmentCompletedMessage message =
        new AppointmentCompletedMessage(42L);

    consumer.consume(message);

    verify(paymentProcessor).process(42L);
}

}