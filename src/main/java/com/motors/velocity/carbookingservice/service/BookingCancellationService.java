package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.config.BookingCancellationProperties;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCancellationService {

    private final BookingRepository bookingRepository;
    private final BookingMetrics bookingMetrics;
    private final BookingCancellationProperties bookingCancellationProperties;

    @Scheduled(fixedDelayString = "${app.booking.cancellation.fixed-delay:60000}")
    @Transactional
    public void cancelUnpaidBankTransferBookings() {
        cancelDueBookings(Instant.now());
    }

    @Transactional
    public int cancelDueBookings(Instant now) {
        int cancelled = 0;
        int safeBatchSize = Math.max(1, bookingCancellationProperties.batchSize());

        while (true) {
            List<CarBooking> dueBookings = bookingRepository.findPendingBankTransferBookingsDueForCancellation(
                    PaymentMode.BANK_TRANSFER, BookingStatus.PENDING_PAYMENT, now, PageRequest.of(0, safeBatchSize));
            if (dueBookings.isEmpty()) {
                return cancelled;
            }

            for (CarBooking booking : dueBookings) {
                int updated = bookingRepository.cancelPendingBankTransfer(
                        booking.getBookingId(), BookingStatus.PENDING_PAYMENT, BookingStatus.CANCELLED, now);
                if (updated == 1) {
                    cancelled++;
                    bookingMetrics.recordBookingCancellation();
                    log.info(
                            "Automatically cancelled unpaid bank transfer bookingId={} rentalStart={} paymentDeadline={}",
                            booking.getBookingId(),
                            booking.getRentalStart(),
                            booking.getPaymentDeadline());
                }
            }

            if (dueBookings.size() < safeBatchSize) {
                return cancelled;
            }
        }
    }
}
