package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.config.PricingProperties;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.model.VehicleCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingPricingService {

    private static final long SECONDS_PER_DAY = Duration.ofDays(1).toSeconds();

    private final PricingProperties pricingProperties;

    public BigDecimal calculateTotalAmount(CarBooking booking) {
        long rentalDays = Math.max(
                1,
                (Duration.between(booking.getRentalStart(), booking.getRentalEnd())
                                        .toSeconds()
                                + SECONDS_PER_DAY
                                - 1)
                        / SECONDS_PER_DAY);

        return dailyRate(booking.getVehicleCategory())
                .multiply(BigDecimal.valueOf(rentalDays))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal dailyRate(VehicleCategory category) {
        return switch (category) {
            case COMPACT -> pricingProperties.compact();
            case SEDAN -> pricingProperties.sedan();
            case SUV -> pricingProperties.suv();
            case LUXURY -> pricingProperties.luxury();
        };
    }
}
