package com.motors.velocity.carbookingservice.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class BookingValidatorTest {

    private final BookingValidator validator = new BookingValidator();

    @Test
    void acceptsRentalWithin21Days() {
        Instant start = Instant.parse("2030-01-01T10:00:00Z");
        Instant end = start.plus(21, ChronoUnit.DAYS);

        assertThatCode(() -> validator.validateRentalPeriod(start, end)).doesNotThrowAnyException();
    }

    @Test
    void rejectsRentalLongerThan21Days() {
        Instant start = Instant.parse("2030-01-01T10:00:00Z");
        Instant end = start.plus(21, ChronoUnit.DAYS).plusSeconds(1);

        assertThatThrownBy(() -> validator.validateRentalPeriod(start, end))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("more than 21 days");
    }

    @Test
    void rejectsEndBeforeOrEqualToStart() {
        Instant start = Instant.parse("2030-01-01T10:00:00Z");
        assertThatThrownBy(() -> validator.validateRentalPeriod(start, start))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("before");
    }
}
