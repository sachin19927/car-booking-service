package com.motors.velocity.carbookingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking.cancellation")
public record BookingCancellationProperties(long fixedDelay, int batchSize) {

    public BookingCancellationProperties() {
        this(60_000L, 500);
    }
}
