package com.motors.velocity.carbookingservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "credit-card-payment")
public record CreditCardPaymentProperties(String baseUrl, Timeout timeout) {

    public record Timeout(Duration connect, Duration read) {}
}
