package com.mediflow.payment;

import org.springframework.stereotype.Component;

import com.mediflow.payment.gateway.PaymentGateway;
import com.mediflow.payment.gateway.PaymentLinkResult;

@Component
public class AppointmentPaymentProcessor {

	private final PaymentService paymentService;
	private final PaymentGateway paymentGateway;

	public AppointmentPaymentProcessor(PaymentService paymentService, PaymentGateway paymentGateway) {
		this.paymentService = paymentService;
		this.paymentGateway = paymentGateway;
	}

	public void process(Long appointmentId) {
		PaymentCreationPlan plan = paymentService.preparePayment(appointmentId);

		if (!plan.requiresGatewayCall()) {
			return;
		}

		PaymentLinkResult result;

		try {
			result = paymentGateway.createPaymentLink(plan.request());
		} catch (RuntimeException exception) {
			paymentService.markPaymentLinkFailed(plan.paymentId(), exception.getMessage());

			throw exception;
		}

		paymentService.markPaymentLinkCreated(plan.paymentId(), result);
	}

}