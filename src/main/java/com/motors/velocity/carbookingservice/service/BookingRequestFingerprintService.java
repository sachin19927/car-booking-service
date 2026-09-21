package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.dto.BookingRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;

@Service
public class BookingRequestFingerprintService {

    public String fingerprint(BookingRequest request) {
        String canonical = String.join(
                "|",
                nullToEmpty(request.customerName()),
                nullToEmpty(request.vehicleId()),
                request.startDate() == null
                        ? ""
                        : request.startDate().toInstant().toString(),
                request.endDate() == null ? "" : request.endDate().toInstant().toString(),
                request.vehicleCategory() == null
                        ? ""
                        : request.vehicleCategory().name(),
                request.paymentMethod() == null ? "" : request.paymentMethod().name(),
                nullToEmpty(request.paymentReference()));

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
