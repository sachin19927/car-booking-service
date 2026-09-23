package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.dto.BookingRequest;
import com.motors.velocity.carbookingservice.dto.BookingResponse;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.exception.ResourceNotFoundException;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class BookingService {

    private final BookingCreationService bookingCreationService;
    private final BookingRepository bookingRepository;
    private final BookingRequestFingerprintService fingerprintService;
    private final BookingMetrics bookingMetrics;

    public BookingResponse createBooking(BookingRequest request, String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String fingerprint = normalizedKey == null ? null : fingerprintService.fingerprint(request);

        if (normalizedKey != null) {
            var existing = bookingRepository.findByIdempotencyKey(normalizedKey);
            if (existing.isPresent()) {
                return replayOrReject(existing.get(), fingerprint);
            }
        }

        try {
            return bookingCreationService.createBooking(request, normalizedKey, fingerprint);
        } catch (DataIntegrityViolationException ex) {
            // A concurrent request with the same idempotency key may win the unique-key race.
            // The transactional creation has already rolled back before this catch block runs.
            if (normalizedKey != null) {
                var existing = bookingRepository.findByIdempotencyKey(normalizedKey);
                if (existing.isPresent()) {
                    return replayOrReject(existing.get(), fingerprint);
                }
            }
            throw ex;
        }
    }

    public BookingResponse getBooking(UUID bookingId) {
        CarBooking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ErrorCode.BOOKING_NOT_FOUND, "Booking not found: " + bookingId));
        return new BookingResponse(booking.getBookingId(), booking.getBookingStatus());
    }

    private BookingResponse replayOrReject(CarBooking existing, String fingerprint) {
        if (!java.util.Objects.equals(existing.getRequestFingerprint(), fingerprint)) {
            throw new BusinessValidationException(
                    ErrorCode.IDEMPOTENCY_KEY_REUSED, "Idempotency-Key was already used with a different request");
        }
        bookingMetrics.recordIdempotentReplay();
        log.info("Returning idempotent booking replay bookingId={}", existing.getBookingId());
        return new BookingResponse(existing.getBookingId(), existing.getBookingStatus());
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 100) {
            throw new BusinessValidationException(
                    ErrorCode.INVALID_IDEMPOTENCY_KEY, "Idempotency-Key must not exceed 100 characters");
        }
        return normalized;
    }
}
