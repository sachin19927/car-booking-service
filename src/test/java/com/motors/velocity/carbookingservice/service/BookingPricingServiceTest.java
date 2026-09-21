package com.motors.velocity.carbookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.motors.velocity.carbookingservice.config.PricingProperties;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.model.VehicleCategory;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BookingPricingServiceTest {

    private final BookingPricingService service = new BookingPricingService(new PricingProperties(
            new BigDecimal("50.00"), new BigDecimal("70.00"), new BigDecimal("90.00"), new BigDecimal("120.00")));

    @Test
    void calculatesTwoRentalDaysForExact48Hours() {
        CarBooking booking = booking(VehicleCategory.SEDAN, "2030-01-01T10:00:00Z", "2030-01-03T10:00:00Z");
        assertThat(service.calculateTotalAmount(booking)).isEqualByComparingTo("140.00");
    }

    @Test
    void roundsPartialRentalDayUpToOneDay() {
        CarBooking booking = booking(VehicleCategory.COMPACT, "2030-01-01T10:00:00Z", "2030-01-02T09:00:00Z");
        assertThat(service.calculateTotalAmount(booking)).isEqualByComparingTo("50.00");
    }

    private CarBooking booking(VehicleCategory category, String start, String end) {
        CarBooking booking = new CarBooking();
        booking.setVehicleCategory(category);
        booking.setRentalStart(Instant.parse(start));
        booking.setRentalEnd(Instant.parse(end));
        return booking;
    }
}
