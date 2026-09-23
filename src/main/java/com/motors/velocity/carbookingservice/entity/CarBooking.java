package com.motors.velocity.carbookingservice.entity;

import com.motors.velocity.carbookingservice.model.BookingConstants;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import com.motors.velocity.carbookingservice.model.VehicleCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "car_booking")
public class CarBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(name = "customer_name", nullable = false, length = 50)
    private String customerName;

    @Column(name = "vehicle_id", nullable = false, length = 9)
    private String vehicleId;

    @Column(nullable = false)
    private Instant rentalStart;

    @Column(nullable = false)
    private Instant rentalEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_category", nullable = false, length = 20)
    private VehicleCategory vehicleCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 30)
    private BookingStatus bookingStatus;

    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "payment_deadline")
    private Instant paymentDeadline;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_received_amount", precision = 19, scale = 2)
    private BigDecimal paymentReceivedAmount;

    @Column(name = "payment_received_at")
    private Instant paymentReceivedAt;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void confirm() {
        this.bookingStatus = BookingStatus.CONFIRMED;
    }

    public void paymentPending() {
        this.bookingStatus = BookingStatus.PENDING_PAYMENT;
    }

    public void setPaymentDeadlineFromRentalStart() {
        this.paymentDeadline = rentalStart.minus(BookingConstants.PAYMENT_DEADLINE_BEFORE_RENTAL);
    }
}
