package com.motors.velocity.carbookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.motors.velocity.carbookingservice.dto.BookingRequest;
import com.motors.velocity.carbookingservice.dto.BookingResponse;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import com.motors.velocity.carbookingservice.model.VehicleCategory;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingServiceIdempotencyTest {

    private final BookingCreationService creationService = mock(BookingCreationService.class);
    private final BookingRepository repository = mock(BookingRepository.class);
    private final BookingRequestFingerprintService fingerprintService = mock(BookingRequestFingerprintService.class);
    private final BookingMetrics metrics = mock(BookingMetrics.class);
    private final BookingService service = new BookingService(creationService, repository, fingerprintService, metrics);

    @Test
    void sameKeyAndSameFingerprintReturnsExistingBooking() {
        BookingRequest request = request("Alice");
        UUID id = UUID.randomUUID();
        CarBooking existing = booking(id, "fp");
        when(fingerprintService.fingerprint(request)).thenReturn("fp");
        when(repository.findByIdempotencyKey("KEY-1")).thenReturn(Optional.of(existing));

        BookingResponse response = service.createBooking(request, "KEY-1");

        assertThat(response.bookingId()).isEqualTo(id);
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        verifyNoInteractions(creationService);
        verify(metrics).recordIdempotentReplay();
    }

    @Test
    void sameKeyWithDifferentFingerprintIsRejected() {
        BookingRequest request = request("Alice");
        when(fingerprintService.fingerprint(request)).thenReturn("new-fp");
        when(repository.findByIdempotencyKey("KEY-1")).thenReturn(Optional.of(booking(UUID.randomUUID(), "old-fp")));

        assertThatThrownBy(() -> service.createBooking(request, "KEY-1"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("different request");
    }

    private BookingRequest request(String customer) {
        return new BookingRequest(
                customer,
                "VH-NL-347",
                ZonedDateTime.parse("2030-01-01T10:00:00Z"),
                ZonedDateTime.parse("2030-01-02T10:00:00Z"),
                VehicleCategory.COMPACT,
                PaymentMode.CASH,
                null);
    }

    private CarBooking booking(UUID id, String fingerprint) {
        CarBooking booking = new CarBooking();
        booking.setBookingId(id);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setRequestFingerprint(fingerprint);
        return booking;
    }
}
