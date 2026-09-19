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

        if (!rentalEnd.isAfter(rentalStart)) {
            throw new BusinessValidationException(
                    ErrorCode.END_DATE_SHORTER_THAN_START_DATE, "Rental end date must be after rental start date");
        }

        Duration duration = Duration.between(rentalStart, rentalEnd);

        if (duration.toDays() > MAX_RENTAL_DAYS) {
            throw new BusinessValidationException(
                    ErrorCode.MAX_RENTAL_DAYS_EXCEEDED, "A vehicle cannot be booked for more than 21 days");
        }
    }
}
