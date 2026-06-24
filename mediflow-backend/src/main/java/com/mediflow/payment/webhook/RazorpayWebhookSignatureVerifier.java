package com.mediflow.payment.webhook;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.mediflow.config.RazorpayProperties;

@Component
public class RazorpayWebhookSignatureVerifier {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private final RazorpayProperties properties;

    public RazorpayWebhookSignatureVerifier(
        RazorpayProperties properties
    ) {
        this.properties = properties;
    }

    public void verify(
        byte[] rawBody,
        String receivedSignature
    ) {
        if (
            !StringUtils.hasText(
                properties.webhookSecret()
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Razorpay webhook secret is not configured"
            );
        }

        if (!StringUtils.hasText(receivedSignature)) {
            throw invalidSignature();
        }

        byte[] expectedSignature = calculateSignature(
            rawBody,
            properties.webhookSecret()
        );

        byte[] receivedSignatureBytes;

        try {
            receivedSignatureBytes = HexFormat.of()
                .parseHex(receivedSignature.strip());
        } catch (IllegalArgumentException exception) {
            throw invalidSignature();
        }

        if (
            !MessageDigest.isEqual(
                expectedSignature,
                receivedSignatureBytes
            )
        ) {
            throw invalidSignature();
        }
    }

    private byte[] calculateSignature(
        byte[] rawBody,
        String webhookSecret
    ) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);

            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA_256
            );

            mac.init(secretKey);

            return mac.doFinal(rawBody);
        } catch (
            NoSuchAlgorithmException
                | InvalidKeyException exception
        ) {
            throw new IllegalStateException(
                "Could not verify Razorpay webhook signature",
                exception
            );
        }
    }

    private ResponseStatusException invalidSignature() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Invalid Razorpay webhook signature"
        );
    }
}