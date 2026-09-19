package com.motors.velocity.carbookingservice.entity;

import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import com.motors.velocity.carbookingservice.model.VehicleCategory;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public void confirm() {
        this.bookingStatus = BookingStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }
}
