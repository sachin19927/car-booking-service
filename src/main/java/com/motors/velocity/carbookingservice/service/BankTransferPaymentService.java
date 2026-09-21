package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.dto.BankTransferPaymentEvent;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.exception.InvalidBankTransferPaymentEventException;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
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
    private final BookingMetrics bookingMetrics;

    @Transactional
    public void processPaymentEvent(BankTransferPaymentEvent event) {
        TransactionDetails details = parseTransactionDetails(event.transactionDetails());
        CarBooking booking = findBooking(details, event);

        if (booking.getPaymentMode() != PaymentMode.BANK_TRANSFER) {
            throw new InvalidBankTransferPaymentEventException(
                    "Payment event does not belong to a bank transfer booking");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            bookingMetrics.recordBankTransferEventIgnored("booking_cancelled");
            log.info("Ignoring late bank transfer payment bookingId={}", booking.getBookingId());
            return;
        }

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            bookingMetrics.recordBankTransferEventIgnored("already_confirmed");
            log.info("Ignoring duplicate bank transfer payment bookingId={}", booking.getBookingId());
            return;
        }

        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new InvalidBankTransferPaymentEventException(
                    "Unsupported booking state for bank transfer payment: " + booking.getBookingStatus());
        }

        int updated = bookingRepository.confirmPendingBankTransfer(
                booking.getBookingId(), event.paymentAmount(), Instant.now());

        if (updated == 1) {
            bookingMetrics.recordBankTransferConfirmed();
            log.info(
                    "Bank transfer payment confirmed bookingId={} paymentId={} amount={}",
                    booking.getBookingId(),
                    event.paymentId(),
                    event.paymentAmount());
        } else {
            bookingMetrics.recordBankTransferEventIgnored("concurrent_state_change");
        }
    }

    private CarBooking findBooking(TransactionDetails details, BankTransferPaymentEvent event) {
        if (details.bookingIdentifier() != null) {
            try {
                UUID bookingId = UUID.fromString(details.bookingIdentifier());
                Optional<CarBooking> booking = bookingRepository.findById(bookingId);
                if (booking.isPresent()) {
                    return booking.get();
                }
            } catch (IllegalArgumentException ignored) {
                // Assignment examples use a 10-character booking identifier; fall back to payment reference.
            }
        }

        return bookingRepository
                .findByPaymentModeAndPaymentReference(PaymentMode.BANK_TRANSFER, details.transactionReference())
                .orElseGet(() -> bookingRepository
                        .findByPaymentModeAndPaymentReference(PaymentMode.BANK_TRANSFER, event.paymentId())
                        .orElseThrow(() -> new InvalidBankTransferPaymentEventException(
                                "No pending booking found for transaction reference "
                                        + details.transactionReference())));
    }

    private TransactionDetails parseTransactionDetails(String value) {
        Matcher matcher = TRANSACTION_DETAILS_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new InvalidBankTransferPaymentEventException(
                    "transactionDetails must have format <TxnRef(12 chars)> <BookingId>");
        }
        return new TransactionDetails(matcher.group(1), matcher.group(2));
    }

    private record TransactionDetails(String transactionReference, String bookingIdentifier) {}
}
