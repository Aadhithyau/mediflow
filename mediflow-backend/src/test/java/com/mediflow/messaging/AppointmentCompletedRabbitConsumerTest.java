package com.mediflow.messaging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mediflow.appointment.AppointmentCompletedEvent;
import com.mediflow.payment.AppointmentPaymentListener;

@ExtendWith(MockitoExtension.class)
class AppointmentCompletedRabbitConsumerTest {

	@Mock
	private AppointmentPaymentListener paymentListener;

	@InjectMocks
	private AppointmentCompletedRabbitConsumer consumer;

	@Test
	void delegatesMessageToPaymentListener() {
		AppointmentCompletedMessage message = new AppointmentCompletedMessage(42L);

		consumer.consume(message);

		verify(paymentListener).handleAppointmentCompleted(new AppointmentCompletedEvent(42L));
	}

}