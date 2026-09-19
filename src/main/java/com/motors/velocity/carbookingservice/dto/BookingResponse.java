package com.motors.velocity.carbookingservice.dto;

import com.motors.velocity.carbookingservice.model.BookingStatus;
import java.util.UUID;

public record BookingResponse(UUID bookingId, BookingStatus status) {}
