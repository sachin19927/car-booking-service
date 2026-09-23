package com.motors.velocity.carbookingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "credit-card-payment")
public record CreditCardPaymentProperties(String baseUrl, Timeout timeout) {

    public record Timeout(Duration connect, Duration read) {}
}
