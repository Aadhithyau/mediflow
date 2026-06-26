package com.mediflow.payment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mediflow.payment.gateway.PaymentGateway;
import com.mediflow.payment.gateway.PaymentGatewayException;
import com.mediflow.payment.gateway.PaymentLinkRequest;
import com.mediflow.payment.gateway.PaymentLinkResult;

@ExtendWith(MockitoExtension.class)
class AppointmentPaymentProcessorTest {

	@Mock
	private PaymentService paymentService;

	@Mock
	private PaymentGateway paymentGateway;

	@InjectMocks
	private AppointmentPaymentProcessor processor;

	@Test
	void createsAndStoresPaymentLink() {
		PaymentLinkRequest request = new PaymentLinkRequest(75000L, "INR", "mediflow-appt-41",
				"MediFlow consultation payment", "Processor Test Patient", "processor-patient@example.com");

		PaymentCreationPlan plan = new PaymentCreationPlan(81L, request);

		PaymentLinkResult result = new PaymentLinkResult("plink_test_81", "https://rzp.io/i/test81");

		when(paymentService.preparePayment(41L)).thenReturn(plan);

		when(paymentGateway.createPaymentLink(request)).thenReturn(result);

		processor.process(41L);

		verify(paymentGateway).createPaymentLink(request);

		verify(paymentService).markPaymentLinkCreated(81L, result);

		verify(paymentService, never()).markPaymentLinkFailed(org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void noActionPlanDoesNotCallGateway() {
		when(paymentService.preparePayment(42L)).thenReturn(PaymentCreationPlan.noAction());

		processor.process(42L);

		verify(paymentService).preparePayment(42L);

		verifyNoInteractions(paymentGateway);

		verify(paymentService, never()).markPaymentLinkCreated(org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any());

		verify(paymentService, never()).markPaymentLinkFailed(org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void gatewayFailureIsRecordedAndRethrown() {
		PaymentLinkRequest request = new PaymentLinkRequest(50000L, "INR", "mediflow-appt-43",
				"MediFlow consultation payment", "Retry Test Patient", "retry-patient@example.com");

		PaymentCreationPlan plan = new PaymentCreationPlan(83L, request);

		when(paymentService.preparePayment(43L)).thenReturn(plan);

		when(paymentGateway.createPaymentLink(request)).thenThrow(new PaymentGatewayException("Razorpay unavailable"));

		assertThatThrownBy(() -> processor.process(43L)).isInstanceOf(PaymentGatewayException.class)
				.hasMessage("Razorpay unavailable");

		verify(paymentService).markPaymentLinkFailed(83L, "Razorpay unavailable");

		verify(paymentService, never()).markPaymentLinkCreated(org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any());
	}

}