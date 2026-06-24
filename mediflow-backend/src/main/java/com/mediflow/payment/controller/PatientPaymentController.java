package com.mediflow.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.payment.PaymentService;
import com.mediflow.payment.dto.PaymentResponse;

@RestController
@RequestMapping("/api/appointments")
public class PatientPaymentController {

    private final PaymentService paymentService;

    public PatientPaymentController(
        PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{appointmentId}/payment")
    public ResponseEntity<PaymentResponse>
        getAppointmentPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long appointmentId
        ) {
        PaymentResponse response =
            paymentService.getPaymentForPatient(
                jwt.getSubject(),
                appointmentId
            );

        return ResponseEntity.ok(response);
    }
}