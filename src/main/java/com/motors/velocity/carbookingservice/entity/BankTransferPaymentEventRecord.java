package com.motors.velocity.carbookingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "bank_transfer_payment_event")
public class BankTransferPaymentEventRecord {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "payment_id", nullable = false, unique = true, length = 100)
    private String paymentId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "payment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
