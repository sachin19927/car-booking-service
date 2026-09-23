package com.motors.velocity.carbookingservice.controller;

import com.motors.velocity.carbookingservice.dto.BookingRequest;
import com.motors.velocity.carbookingservice.dto.BookingResponse;
import com.motors.velocity.carbookingservice.service.BookingService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest bookingRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(bookingRequest, idempotencyKey));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId));
    }
}
