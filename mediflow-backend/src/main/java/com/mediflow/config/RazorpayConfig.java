package com.mediflow.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(
RazorpayProperties.class
)
public class RazorpayConfig {

@Bean
RestClient razorpayRestClient(
    RazorpayProperties properties
) {
    if (!StringUtils.hasText(properties.apiBaseUrl())) {
        throw new IllegalStateException(
            "Razorpay API base URL must be configured"
        );
    }

    return RestClient
        .builder()
        .baseUrl(properties.apiBaseUrl())
        .build();
}

}
