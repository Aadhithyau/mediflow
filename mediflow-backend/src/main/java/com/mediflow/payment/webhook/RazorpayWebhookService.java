package com.mediflow.payment.webhook;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.mediflow.appointment.AppointmentStatus;
import com.mediflow.payment.Payment;
import com.mediflow.payment.PaymentRepository;
import com.mediflow.payment.PaymentStatus;

@Service
public class RazorpayWebhookService {

private static final String PAYMENT_LINK_PAID_EVENT =
    "payment_link.paid";

private static final int MAX_EVENT_ID_LENGTH = 100;

private final RazorpayWebhookSignatureVerifier
    signatureVerifier;

private final PaymentRepository paymentRepository;
private final ObjectMapper objectMapper;

public RazorpayWebhookService(
    RazorpayWebhookSignatureVerifier signatureVerifier,
    PaymentRepository paymentRepository,
    ObjectMapper objectMapper
) {
    this.signatureVerifier = signatureVerifier;
    this.paymentRepository = paymentRepository;
    this.objectMapper = objectMapper;
}

@Transactional
public void processWebhook(
    byte[] rawBody,
    String receivedSignature,
    String webhookEventId
) {
    signatureVerifier.verify(
        rawBody,
        receivedSignature
    );

    String eventId = validateEventId(webhookEventId);
    JsonNode root = parseBody(rawBody);

    String eventType = requiredText(root, "event");

    if (!PAYMENT_LINK_PAID_EVENT.equals(eventType)) {
        return;
    }

    if (
        paymentRepository
            .existsByRazorpayWebhookEventId(eventId)
    ) {
        return;
    }

    JsonNode payload = root.path("payload");

    JsonNode paymentLinkEntity = payload
        .path("payment_link")
        .path("entity");

    JsonNode paymentEntity = payload
        .path("payment")
        .path("entity");

    String paymentLinkId = requiredText(
        paymentLinkEntity,
        "id"
    );

    String referenceId = requiredText(
        paymentLinkEntity,
        "reference_id"
    );

    String paymentLinkStatus = requiredText(
        paymentLinkEntity,
        "status"
    );

    String paymentLinkCurrency = requiredText(
        paymentLinkEntity,
        "currency"
    );

    long paymentLinkAmount = requiredLong(
        paymentLinkEntity,
        "amount"
    );

    long paymentLinkAmountPaid = requiredLong(
        paymentLinkEntity,
        "amount_paid"
    );

    String razorpayPaymentId = requiredText(
        paymentEntity,
        "id"
    );

    String paymentStatus = requiredText(
        paymentEntity,
        "status"
    );

    String paymentCurrency = requiredText(
        paymentEntity,
        "currency"
    );

    long paymentAmount = requiredLong(
        paymentEntity,
        "amount"
    );

    boolean captured = requiredBoolean(
        paymentEntity,
        "captured"
    );

    long paymentCreatedAt = requiredLong(
        paymentEntity,
        "created_at"
    );

    Payment payment = paymentRepository
        .findByPaymentLinkIdForUpdate(paymentLinkId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Webhook payment link is not known "
                + "to MediFlow"
        ));

    if (payment.getStatus() == PaymentStatus.PAID) {
        if (
            razorpayPaymentId.equals(
                payment.getRazorpayPaymentId()
            )
        ) {
            return;
        }

        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Payment has already been completed "
                + "with a different Razorpay payment"
        );
    }

    if (
        payment.getStatus()
            != PaymentStatus.PAYMENT_PENDING
    ) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Payment is not awaiting Razorpay confirmation"
        );
    }

    if (
        payment.getAppointment().getStatus()
            != AppointmentStatus.COMPLETED
    ) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "The related appointment is not completed"
        );
    }

    validatePaidPayload(
        payment,
        referenceId,
        paymentLinkStatus,
        paymentLinkCurrency,
        paymentLinkAmount,
        paymentLinkAmountPaid,
        paymentStatus,
        paymentCurrency,
        paymentAmount,
        captured
    );

    payment.setStatus(PaymentStatus.PAID);
    payment.setRazorpayPaymentId(razorpayPaymentId);
    payment.setRazorpayWebhookEventId(eventId);
    payment.setFailureMessage(null);
    payment.setPaidAt(
        OffsetDateTime.ofInstant(
            Instant.ofEpochSecond(paymentCreatedAt),
            ZoneOffset.UTC
        )
    );

    paymentRepository.saveAndFlush(payment);
}

private void validatePaidPayload(
    Payment payment,
    String referenceId,
    String paymentLinkStatus,
    String paymentLinkCurrency,
    long paymentLinkAmount,
    long paymentLinkAmountPaid,
    String paymentStatus,
    String paymentCurrency,
    long paymentAmount,
    boolean captured
) {
    if (
        !"paid".equalsIgnoreCase(paymentLinkStatus)
            || !"captured".equalsIgnoreCase(
                paymentStatus
            )
            || !captured
    ) {
        throw invalidPayload(
            "Razorpay payment is not captured"
        );
    }

    if (
        !payment.getRazorpayReferenceId()
            .equals(referenceId)
    ) {
        throw invalidPayload(
            "Razorpay reference ID does not match"
        );
    }

    long expectedAmount = toSmallestCurrencyUnit(
        payment.getAmount()
    );

    if (
        paymentLinkAmount != expectedAmount
            || paymentLinkAmountPaid != expectedAmount
            || paymentAmount != expectedAmount
    ) {
        throw invalidPayload(
            "Razorpay payment amount does not match"
        );
    }

    if (
        !payment.getCurrency().equalsIgnoreCase(
            paymentLinkCurrency
        )
            || !payment.getCurrency().equalsIgnoreCase(
                paymentCurrency
            )
    ) {
        throw invalidPayload(
            "Razorpay payment currency does not match"
        );
    }
}

private long toSmallestCurrencyUnit(
    BigDecimal amount
) {
    try {
        return amount
            .movePointRight(2)
            .longValueExact();
    } catch (ArithmeticException exception) {
        throw new IllegalStateException(
            "Stored payment amount is invalid",
            exception
        );
    }
}

private JsonNode parseBody(byte[] rawBody) {
    try {
        return objectMapper.readTree(rawBody);
    } catch (JacksonException exception) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Razorpay webhook body is invalid",
            exception
        );
    }
}

private String validateEventId(
    String webhookEventId
) {
    if (!StringUtils.hasText(webhookEventId)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Razorpay webhook event ID is missing"
        );
    }

    String eventId = webhookEventId.strip();

    if (eventId.length() > MAX_EVENT_ID_LENGTH) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Razorpay webhook event ID is too long"
        );
    }

    return eventId;
}

private String requiredText(
    JsonNode parent,
    String fieldName
) {
    JsonNode value = parent.path(fieldName);

    if (
        !value.isTextual()
            || !StringUtils.hasText(value.asText())
    ) {
        throw invalidPayload(
            "Required Razorpay field is missing: "
                + fieldName
        );
    }

    return value.asText();
}

private long requiredLong(
    JsonNode parent,
    String fieldName
) {
    JsonNode value = parent.path(fieldName);

    if (!value.canConvertToLong()) {
        throw invalidPayload(
            "Required Razorpay number is missing: "
                + fieldName
        );
    }

    return value.longValue();
}

private boolean requiredBoolean(
    JsonNode parent,
    String fieldName
) {
    JsonNode value = parent.path(fieldName);

    if (!value.isBoolean()) {
        throw invalidPayload(
            "Required Razorpay boolean is missing: "
                + fieldName
        );
    }

    return value.booleanValue();
}

private ResponseStatusException invalidPayload(
    String message
) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        message
    );
}

}
