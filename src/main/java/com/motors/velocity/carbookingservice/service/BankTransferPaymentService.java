package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.dto.BankTransferPaymentEvent;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.exception.InvalidBankTransferPaymentEventException;
import com.motors.velocity.carbookingservice.model.BookingConstants;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import com.motors.velocity.carbookingservice.repository.BankTransferPaymentEventRepository;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankTransferPaymentService {

    private static final Pattern TRANSACTION_DETAILS_PATTERN = Pattern.compile("^(\\S{12})\\s+([A-Za-z0-9-]{10,36})$");

    private final BookingRepository bookingRepository;
    private final BankTransferPaymentEventRepository paymentEventRepository;
    private final BookingMetrics bookingMetrics;

    @Transactional
    public void processPaymentEvent(BankTransferPaymentEvent event) {
        TransactionDetails details = parseTransactionDetails(event.transactionDetails());
        CarBooking booking = findBooking(details, event);

        if (shouldIgnoreBookingState(booking)) {
            return;
        }
        validateBookingState(booking);
        Instant receivedAt = Instant.now();

        if (isDuplicatePaymentEvent(event, booking, receivedAt)) {
            return;
        }

        if (!applyPayment(event, booking, receivedAt)) {
            bookingMetrics.recordBankTransferEventIgnored("concurrent_state_change");
            return;
        }

        logPaymentOutcome(event, reloadBooking(booking));
    }

    private CarBooking findBooking(TransactionDetails details, BankTransferPaymentEvent event) {
        Optional<CarBooking> bookingById = findBookingByIdentifer(details.bookingIdentifier());
        if (bookingById.isPresent()) {
            return bookingById.get();
        }

        return findBookingByReference(details.transactionReference(), event.paymentId())
                .orElseThrow(() -> new InvalidBankTransferPaymentEventException(
                        "No pending booking found for transaction reference " + details.transactionReference()));
    }

    private Optional<CarBooking> findBookingByIdentifer(String bookingIdentifier) {
        if (bookingIdentifier == null) {
            return Optional.empty();
        }
        try {
            return bookingRepository.findById(UUID.fromString(bookingIdentifier));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Optional<CarBooking> findBookingByReference(String transactionReference, String paymentId) {
        return bookingRepository
                .findByPaymentModeAndPaymentReference(PaymentMode.BANK_TRANSFER, transactionReference)
                .or(() -> bookingRepository.findByPaymentModeAndPaymentReference(PaymentMode.BANK_TRANSFER, paymentId));
    }

    private TransactionDetails parseTransactionDetails(String value) {
        Matcher matcher = TRANSACTION_DETAILS_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new InvalidBankTransferPaymentEventException(BookingConstants.TRANSACTION_DETAILS_FORMAT_MESSAGE);
        }
        return new TransactionDetails(matcher.group(1), matcher.group(2));
    }

    private boolean shouldIgnoreBookingState(CarBooking booking) {
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            bookingMetrics.recordBankTransferEventIgnored("booking_cancelled");
            log.info("Ignoring late bank transfer payment bookingId={}", booking.getBookingId());
            return true;
        }

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            bookingMetrics.recordBankTransferEventIgnored("already_confirmed");
            log.info("Ignoring duplicate bank transfer payment bookingId={}", booking.getBookingId());
            return true;
        }
        return false;
    }

    private void validateBookingState(CarBooking booking) {
        if (booking.getPaymentMode() != PaymentMode.BANK_TRANSFER) {
            throw new InvalidBankTransferPaymentEventException(
                    "Payment event does not belong to a bank transfer booking");
        }

        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new InvalidBankTransferPaymentEventException(
                    "Unsupported booking state for bank transfer payment: " + booking.getBookingStatus());
        }
    }

    private boolean isDuplicatePaymentEvent(BankTransferPaymentEvent event, CarBooking booking, Instant receivedAt) {
        int inserted = paymentEventRepository.insertIfAbsent(
                UUID.randomUUID(), event.paymentId(), booking.getBookingId(), event.paymentAmount(), receivedAt);

        if (inserted == 0) {
            bookingMetrics.recordBankTransferEventIgnored("duplicate_payment_event");
            log.info(
                    "Ignoring duplicate bank transfer paymentId={} bookingId={}",
                    event.paymentId(),
                    booking.getBookingId());
            return true;
        }
        return false;
    }

    private boolean applyPayment(BankTransferPaymentEvent event, CarBooking booking, Instant receivedAt) {
        return bookingRepository.applyBankTransferPayment(
                        booking.getBookingId(),
                        event.paymentAmount(),
                        BookingStatus.CONFIRMED,
                        BookingStatus.PENDING_PAYMENT,
                        receivedAt)
                == 1;
    }

    private CarBooking reloadBooking(CarBooking booking) {
        return bookingRepository.findById(booking.getBookingId()).orElseThrow();
    }

    private void logPaymentOutcome(BankTransferPaymentEvent event, CarBooking updatedBooking) {
        if (updatedBooking.getBookingStatus() == BookingStatus.CONFIRMED) {
            bookingMetrics.recordBankTransferConfirmed();
            log.info(
                    "Bank transfer payment fully confirmed bookingId={} paymentId={} amountReceived={} totalAmount={}",
                    updatedBooking.getBookingId(),
                    event.paymentId(),
                    updatedBooking.getPaymentReceivedAmount(),
                    updatedBooking.getTotalAmount());
            return;
        }
        bookingMetrics.recordBankTransferPartialPayment();
        log.info(
                "Bank transfer partial payment recorded bookingId={} paymentId={} amountReceived={} totalAmount={}",
                updatedBooking.getBookingId(),
                event.paymentId(),
                updatedBooking.getPaymentReceivedAmount(),
                updatedBooking.getTotalAmount());
    }

    private record TransactionDetails(String transactionReference, String bookingIdentifier) {}
}
