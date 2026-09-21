package com.motors.velocity.carbookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.motors.velocity.carbookingservice.dto.BookingRequest;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import com.motors.velocity.carbookingservice.model.VehicleCategory;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class BookingRequestFingerprintServiceTest {

    private final BookingRequestFingerprintService service = new BookingRequestFingerprintService();

    @Test
    void sameBusinessRequestProducesSameFingerprint() {
        BookingRequest first = request("2030-01-01T10:00:00Z", "2030-01-02T10:00:00Z");
        BookingRequest second = request("2030-01-01T11:00:00+01:00", "2030-01-02T11:00:00+01:00");

        assertThat(service.fingerprint(first)).isEqualTo(service.fingerprint(second));
    }

    @Test
    void differentRequestProducesDifferentFingerprint() {
        BookingRequest first = request("2030-01-01T10:00:00Z", "2030-01-02T10:00:00Z");
        BookingRequest second = new BookingRequest(
                "Different",
                "VH-NL-347",
                first.startDate(),
                first.endDate(),
                VehicleCategory.COMPACT,
                PaymentMode.CASH,
                null);

        assertThat(service.fingerprint(first)).isNotEqualTo(service.fingerprint(second));
    }

    private BookingRequest request(String start, String end) {
        return new BookingRequest(
                "Alice",
                "VH-NL-347",
                ZonedDateTime.parse(start),
                ZonedDateTime.parse(end),
                VehicleCategory.COMPACT,
                PaymentMode.CASH,
                null);
    }
}
