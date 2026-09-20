package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class BookingValidator {

    private static final long MAX_RENTAL_DAYS = 21;

    public void validateRentalPeriod(Instant rentalStart, Instant rentalEnd) {

        if (!rentalStart.isBefore(rentalEnd)) {
            throw new BusinessValidationException(
                    ErrorCode.INVALID_RENTAL_PERIOD,
                    "Rental start date and time must be before rental end date and time");
        }

        Duration duration = Duration.between(rentalStart, rentalEnd);

        if (duration.toDays() > MAX_RENTAL_DAYS) {
            throw new BusinessValidationException(
                    ErrorCode.MAX_RENTAL_DAYS_EXCEEDED, "A vehicle cannot be booked for more than 21 days");
        }
    }
}
