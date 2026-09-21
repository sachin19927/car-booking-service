package com.motors.velocity.carbookingservice.repository;

import com.motors.velocity.carbookingservice.entity.BankTransferPaymentEventRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankTransferPaymentEventRepository extends JpaRepository<BankTransferPaymentEventRecord, UUID> {

    @Modifying
    @Query(value = """
                    INSERT INTO bank_transfer_payment_event
                        (event_id, payment_id, booking_id, payment_amount, received_at)
                    VALUES (:eventId, :paymentId, :bookingId, :paymentAmount, :receivedAt)
                    ON CONFLICT (payment_id) DO NOTHING
                    """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("paymentId") String paymentId,
            @Param("bookingId") UUID bookingId,
            @Param("paymentAmount") BigDecimal paymentAmount,
            @Param("receivedAt") Instant receivedAt);
}
