package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.model.BookingConstants;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class BookingValidator {

    private static final Duration MAX_RENTAL_DURATION = Duration.ofDays(BookingConstants.MAX_RENTAL_DAYS);

    public void validateRentalPeriod(Instant rentalStart, Instant rentalEnd) {

        if (rentalStart.isBefore(Instant.now())) {
            throw new BusinessValidationException(
                    ErrorCode.INVALID_RENTAL_PERIOD, "Start date cannot be previous dates");
        }

        if (!rentalStart.isBefore(rentalEnd)) {
            throw new BusinessValidationException(
                    ErrorCode.INVALID_RENTAL_PERIOD,
                    "Rental start date and time must be before rental end date and time");
        }

        Duration duration = Duration.between(rentalStart, rentalEnd);

        if (duration.compareTo(MAX_RENTAL_DURATION) > 0) {
            throw new BusinessValidationException(
                    ErrorCode.MAX_RENTAL_DAYS_EXCEEDED, "A vehicle cannot be booked for more than 21 days");
        }
    }
}
