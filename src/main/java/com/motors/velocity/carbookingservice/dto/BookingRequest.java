package com.motors.velocity.carbookingservice.dto;

import com.motors.velocity.carbookingservice.model.PaymentMode;
import com.motors.velocity.carbookingservice.model.VehicleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.ZonedDateTime;

public record BookingRequest(
        @NotBlank(message = "Customer name must not be blank")
        @Size(max = 50, message = "Customer name must not exceed 50 characters")
        String customerName,

        @NotBlank(message = "Vehicle Id must not be blank")
        @Size(max = 9, message = "Vehicle Id must not exceed 9 characters")
        @Pattern(
                regexp = "^(VH-NL-[0-9]{3}|[0-9]{3}-NL-[A-Z]{2}|NL-[0-9]{3}-[A-Z]{2})$",
                message = "Vehicle ID must be in a valid format")
        String vehicleId,

        @NotNull(message = "Start date must not be null") ZonedDateTime startDate,
        @NotNull(message = "End date must not be null") ZonedDateTime endDate,

        @NotNull(message = "Vehicle category must not be null")
        VehicleCategory vehicleCategory,

        @NotNull(message = "Payment method must not be null")
        PaymentMode paymentMethod,

        @Size(max = 100, message = "Payment reference must not exceed 100 characters")
        String paymentReference) {}
