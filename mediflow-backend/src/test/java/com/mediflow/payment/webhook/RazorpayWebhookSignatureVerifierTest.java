package com.mediflow.payment.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.config.RazorpayProperties;

class RazorpayWebhookSignatureVerifierTest {

private static final String WEBHOOK_SECRET =
    "test-webhook-secret";

@Test
void validWebhookSignatureIsAccepted()
    throws Exception {

    byte[] rawBody =
        "{\"event\":\"payment_link.paid\"}"
            .getBytes(StandardCharsets.UTF_8);

    String signature = createSignature(
        rawBody,
        WEBHOOK_SECRET
    );

    RazorpayWebhookSignatureVerifier verifier =
        createVerifier(WEBHOOK_SECRET);

    assertThatCode(
        () -> verifier.verify(rawBody, signature)
    )
        .doesNotThrowAnyException();
}

@Test
void invalidWebhookSignatureIsRejected() {
    byte[] rawBody =
        "{\"event\":\"payment_link.paid\"}"
            .getBytes(StandardCharsets.UTF_8);

    RazorpayWebhookSignatureVerifier verifier =
        createVerifier(WEBHOOK_SECRET);

    assertThatThrownBy(
        () -> verifier.verify(
            rawBody,
            "0011223344556677"
        )
    )
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> {
            ResponseStatusException responseException =
                (ResponseStatusException) exception;

            assertThat(responseException.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

            assertThat(responseException.getReason())
                .isEqualTo(
                    "Invalid Razorpay webhook signature"
                );
        });
}

@Test
void missingWebhookSecretReturnsServiceUnavailable() {
    byte[] rawBody =
        "{}".getBytes(StandardCharsets.UTF_8);

    RazorpayWebhookSignatureVerifier verifier =
        createVerifier("");

    assertThatThrownBy(
        () -> verifier.verify(rawBody, "00")
    )
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(exception -> {
            ResponseStatusException responseException =
                (ResponseStatusException) exception;

            assertThat(responseException.getStatusCode())
                .isEqualTo(
                    HttpStatus.SERVICE_UNAVAILABLE
                );

            assertThat(responseException.getReason())
                .isEqualTo(
                    "Razorpay webhook secret "
                        + "is not configured"
                );
        });
}

private RazorpayWebhookSignatureVerifier createVerifier(
    String webhookSecret
) {
    RazorpayProperties properties =
        new RazorpayProperties(
            "",
            "",
            webhookSecret,
            "https://api.razorpay.com"
        );

    return new RazorpayWebhookSignatureVerifier(
        properties
    );
}

private String createSignature(
    byte[] rawBody,
    String secret
) throws Exception {

    Mac mac = Mac.getInstance("HmacSHA256");

    mac.init(
        new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        )
    );

    return HexFormat.of().formatHex(
        mac.doFinal(rawBody)
    );
}

}
