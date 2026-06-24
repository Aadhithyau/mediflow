package com.mediflow.payment.gateway;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mediflow.config.RazorpayProperties;

@Component
public class RazorpayPaymentGateway
    implements PaymentGateway {

    private final RestClient restClient;
    private final RazorpayProperties properties;

    public RazorpayPaymentGateway(
        RestClient restClient,
        RazorpayProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public PaymentLinkResult createPaymentLink(
        PaymentLinkRequest request
    ) {
        validateCredentials();

        RazorpayCreatePaymentLinkRequest payload =
            new RazorpayCreatePaymentLinkRequest(
                request.amountInSmallestUnit(),
                request.currency(),
                false,
                request.referenceId(),
                request.description(),
                new RazorpayCustomer(
                    request.customerName(),
                    request.customerEmail()
                ),
                new RazorpayNotify(false, false),
                false
            );

        try {
            RazorpayCreatePaymentLinkResponse response =
                restClient
                    .post()
                    .uri("/v1/payment_links")
                    .headers(headers ->
                        headers.setBasicAuth(
                            properties.keyId(),
                            properties.keySecret()
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(
                        RazorpayCreatePaymentLinkResponse.class
                    );

            if (
                response == null
                    || !StringUtils.hasText(response.id())
                    || !StringUtils.hasText(
                        response.shortUrl()
                    )
            ) {
                throw new PaymentGatewayException(
                    "Razorpay returned an incomplete "
                        + "payment-link response"
                );
            }

            return new PaymentLinkResult(
                response.id(),
                response.shortUrl()
            );
        } catch (
            RestClientResponseException exception
        ) {
            throw new PaymentGatewayException(
                "Razorpay rejected payment-link creation "
                    + "with HTTP status "
                    + exception.getStatusCode().value(),
                exception
            );
        } catch (RestClientException exception) {
            throw new PaymentGatewayException(
                "Razorpay payment-link request failed",
                exception
            );
        }
    }

    private void validateCredentials() {
        if (
            !StringUtils.hasText(properties.keyId())
                || !StringUtils.hasText(
                    properties.keySecret()
                )
        ) {
            throw new PaymentGatewayException(
                "Razorpay API credentials are not configured"
            );
        }
    }

    private record RazorpayCreatePaymentLinkRequest(
        long amount,
        String currency,

        @JsonProperty("accept_partial")
        boolean acceptPartial,

        @JsonProperty("reference_id")
        String referenceId,

        String description,
        RazorpayCustomer customer,

        @JsonProperty("notify")
        RazorpayNotify notifications,

        @JsonProperty("reminder_enable")
        boolean reminderEnable
    ) {
    }

    private record RazorpayCustomer(
        String name,
        String email
    ) {
    }

    private record RazorpayNotify(
        boolean sms,
        boolean email
    ) {
    }

    private record RazorpayCreatePaymentLinkResponse(
        String id,

        @JsonProperty("short_url")
        String shortUrl
    ) {
    }
}