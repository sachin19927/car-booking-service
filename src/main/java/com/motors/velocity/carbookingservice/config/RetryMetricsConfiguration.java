package com.motors.velocity.carbookingservice.config;

import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RetryMetricsConfiguration {

    private final RetryRegistry retryRegistry;
    private final BookingMetrics bookingMetrics;

    @PostConstruct
    void registerRetryMetrics() {
        retryRegistry
                .retry("creditCardPayment")
                .getEventPublisher()
                .onRetry(event -> bookingMetrics.recordCreditCardRetry())
                .onError(event -> bookingMetrics.recordCreditCardRetryExhausted())
                .onSuccess(event -> {
                    if (event.getNumberOfRetryAttempts() > 0) {
                        bookingMetrics.recordCreditCardRetrySuccess();
                    }
                });
    }
}
