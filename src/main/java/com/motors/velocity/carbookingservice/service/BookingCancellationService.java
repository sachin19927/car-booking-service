package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCancellationService {

    private final BookingRepository bookingRepository;
    private final BookingMetrics bookingMetrics;

    @Scheduled(fixedDelayString = "${app.booking.cancellation.fixed-delay:60000}")
    @Transactional
    public void cancelUnpaidBankTransferBookings() {
        Instant now = Instant.now();
        List<CarBooking> dueBookings = bookingRepository.findPendingBankTransferBookingsDueForCancellation(now);

        for (CarBooking booking : dueBookings) {
            int updated = bookingRepository.cancelPendingBankTransfer(booking.getBookingId(), now);
            if (updated == 1) {
                bookingMetrics.recordBookingCancellation();
                log.info(
                        "Automatically cancelled unpaid bank transfer bookingId={} rentalStart={} paymentDeadline={}",
                        booking.getBookingId(),
                        booking.getRentalStart(),
                        booking.getPaymentDeadline());
            }
        }
    }
}
