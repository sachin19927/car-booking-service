package com.motors.velocity.carbookingservice.observability;

import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class BookingMetrics {

    private static final String BOOKING_CREATED_TOTAL = "booking.created.total";
    private static final String BOOKING_FAILURE_TOTAL = "booking.failure.total";
    private static final String BOOKING_CANCELLATION_TOTAL = "booking.cancellation.total";
    private static final String BOOKING_IDEMPOTENCY_REPLAY_TOTAL = "booking.idempotency.replay.total";
    private static final String CREDIT_CARD_PAYMENT_TOTAL = "credit_card.payment.total";
    private static final String CREDIT_CARD_PAYMENT_RETRY_TOTAL = "credit_card.payment.retry.total";
    private static final String CREDIT_CARD_PAYMENT_RETRY_EXHAUSTED_TOTAL = "credit_card.payment.retry.exhausted.total";
    private static final String CREDIT_CARD_PAYMENT_RETRY_SUCCESS_TOTAL = "credit_card.payment.retry.success.total";
    private static final String BANK_TRANSFER_PAYMENT_EVENT_TOTAL = "bank_transfer.payment_event.total";
    private static final String BANK_TRANSFER_PAYMENT_EVENT_FAILURE_TOTAL = "bank_transfer.payment_event.failure.total";
    private static final String BANK_TRANSFER_PAYMENT_EVENT_IGNORED_TOTAL = "bank_transfer.payment_event.ignored.total";

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> bookingCreatedCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> bookingFailureCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> creditCardOutcomeCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> bankTransferEventCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> bookingCancellationCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> bankTransferFailureCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> bankTransferIgnoredCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> creditCardRetryCounters = new ConcurrentHashMap<>();

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
                        key -> Counter.builder(BOOKING_CREATED_TOTAL)
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
                        key -> Counter.builder(BOOKING_FAILURE_TOTAL)
                                .description("Number of booking creation failures")
                                .tag("error_type", errorType)
                                .register(meterRegistry))
                .increment();
    }

    public void recordBookingCancellation() {
        bookingCancellationCounters
                .computeIfAbsent(
                        "payment_deadline",
                        key -> Counter.builder(BOOKING_CANCELLATION_TOTAL)
                                .description("Number of bank transfer bookings automatically cancelled")
                                .tag("reason", key)
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
                        key -> Counter.builder(CREDIT_CARD_PAYMENT_TOTAL)
                                .description("Credit card payment validation outcomes")
                                .tag("outcome", outcome)
                                .register(meterRegistry))
                .increment();
    }

    public void recordCreditCardRetry() {
        creditCardRetryCounters
                .computeIfAbsent(
                        CREDIT_CARD_PAYMENT_RETRY_TOTAL,
                        key -> Counter.builder(key)
                                .description("Number of credit card payment retry attempts")
                                .register(meterRegistry))
                .increment();
    }

    public void recordCreditCardRetryExhausted() {
        creditCardRetryCounters
                .computeIfAbsent(
                        CREDIT_CARD_PAYMENT_RETRY_EXHAUSTED_TOTAL,
                        key -> Counter.builder(key)
                                .description("Number of credit card payment retry sequences exhausted")
                                .tag("outcome", key)
                                .register(meterRegistry))
                .increment();
    }

    public void recordCreditCardRetrySuccess() {
        creditCardRetryCounters
                .computeIfAbsent(
                        CREDIT_CARD_PAYMENT_RETRY_SUCCESS_TOTAL,
                        key -> Counter.builder(key)
                                .description("Number of credit card payment retry sequences eventually successful")
                                .register(meterRegistry))
                .increment();
    }

    public void recordBankTransferEventReceived() {
        bankTransferEventCounters
                .computeIfAbsent(
                        "received",
                        key -> Counter.builder(BANK_TRANSFER_PAYMENT_EVENT_TOTAL)
                                .description("Bank transfer payment events received")
                                .tag("outcome", "received")
                                .register(meterRegistry))
                .increment();
    }

    public void recordBankTransferEventProcessed() {
        recordBankTransferEventCounter("processed");
    }

    public void recordBankTransferEventFailed(String reason) {
        bankTransferFailureCounters
                .computeIfAbsent(
                        reason,
                        key -> Counter.builder(BANK_TRANSFER_PAYMENT_EVENT_FAILURE_TOTAL)
                                .description("Bank transfer payment event processing failures")
                                .tag("reason", key)
                                .register(meterRegistry))
                .increment();
    }

    public void recordBankTransferEventIgnored(String reason) {
        bankTransferIgnoredCounters
                .computeIfAbsent(
                        reason,
                        key -> Counter.builder(BANK_TRANSFER_PAYMENT_EVENT_IGNORED_TOTAL)
                                .description("Bank transfer payment events intentionally ignored")
                                .tag("reason", key)
                                .register(meterRegistry))
                .increment();
    }

    public void recordBankTransferConfirmed() {
        recordBankTransferEventCounter("confirmed");
    }

    public void recordBankTransferPartialPayment() {
        recordBankTransferEventCounter("partial_payment");
    }

    public void recordIdempotentReplay() {
        creditCardRetryCounters
                .computeIfAbsent(
                        BOOKING_IDEMPOTENCY_REPLAY_TOTAL,
                        key -> Counter.builder(key)
                                .description("Number of booking requests served from an existing idempotent booking")
                                .register(meterRegistry))
                .increment();
    }

    private void recordBankTransferEventCounter(String outcome) {
        bankTransferEventCounters
                .computeIfAbsent(
                        outcome,
                        key -> Counter.builder(BANK_TRANSFER_PAYMENT_EVENT_TOTAL)
                                .description("Bank transfer payment event processing outcomes")
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
