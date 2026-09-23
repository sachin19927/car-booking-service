package com.motors.velocity.carbookingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaProperties(
        @DefaultValue Listener listener,
        @DefaultValue Consumer consumer,
        @DefaultValue BankTransferPaymentEvents bankTransferPaymentEvents) {
    public record Listener(@DefaultValue("3") int concurrency) {}

    public record Consumer(
            @DefaultValue("car-booking-service") String groupId,
            @DefaultValue Retry retry) {}

    public record Retry(
            @DefaultValue("3") int maxAttempts,
            @DefaultValue("1s") Duration initialBackoff) {}

    public record BankTransferPaymentEvents(
            @DefaultValue("bank-transfer-payment-events") String topic,

            @DefaultValue("bank-transfer-payment-events.DLT")
            String dltTopic,

            @DefaultValue("3") int partitions,
            @DefaultValue("1") short replicationFactor) {}
}
