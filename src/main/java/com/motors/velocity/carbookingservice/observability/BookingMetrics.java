package com.motors.velocity.carbookingservice.observability;

import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> bookingCreatedCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> bookingFailureCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> creditCardOutcomeCounters = new ConcurrentHashMap<>();

    public Timer.Sample startBookingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopBookingTimer(Timer.Sample sample) {
        sample.stop(bookingCreationTimer());
    }

    public void recordBookingCreated(PaymentMode paymentMode, BookingStatus status) {
        bookingCreatedCounters
                .computeIfAbsent(
                        paymentMode.name() + ':' + status.name(),
                        key -> Counter.builder("booking.created.total")
                                .description("Number of bookings successfully created")
                                .tag("payment_mode", paymentMode.name())
                                .tag("booking_status", status.name())
                                .register(meterRegistry))
                .increment();
    }

    public void recordBookingFailure(String errorType) {
        bookingFailureCounters
                .computeIfAbsent(
                        errorType,
                        key -> Counter.builder("booking.failure.total")
                                .description("Number of booking creation failures")
                                .tag("error_type", errorType)
                                .register(meterRegistry))
                .increment();
    }

    public Timer.Sample startCreditCardTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopCreditCardTimer(Timer.Sample sample) {
        sample.stop(creditCardPaymentTimer());
    }

    public void recordCreditCardOutcome(String outcome) {
        creditCardOutcomeCounters
                .computeIfAbsent(
                        outcome,
                        key -> Counter.builder("credit_card.payment.total")
                                .description("Credit card payment validation outcomes")
                                .tag("outcome", outcome)
                                .register(meterRegistry))
                .increment();
    }

    private Timer bookingCreationTimer() {
        return Timer.builder("booking.creation.duration")
                .description("Time taken to create a booking")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    private Timer creditCardPaymentTimer() {
        return Timer.builder("credit_card.payment.duration")
                .description("Time taken for one credit card payment validation attempt")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }
}
