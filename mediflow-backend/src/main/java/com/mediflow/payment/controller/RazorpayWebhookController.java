package com.mediflow.payment.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediflow.payment.webhook.RazorpayWebhookService;

@RestController
@RequestMapping("/api/payments/webhooks")
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    public RazorpayWebhookController(
        RazorpayWebhookService webhookService
    ) {
        this.webhookService = webhookService;
    }

    @PostMapping(
        value = "/razorpay",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> handleRazorpayWebhook(
        @RequestBody byte[] rawBody,
        @RequestHeader(
            name = "X-Razorpay-Signature",
            required = false
        )
        String signature,
        @RequestHeader(
            name = "X-Razorpay-Event-Id",
            required = false
        )
        String eventId
    ) {
        webhookService.processWebhook(
            rawBody,
            signature,
            eventId
        );

        return ResponseEntity.noContent().build();
    }
}