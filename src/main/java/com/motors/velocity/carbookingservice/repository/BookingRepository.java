package com.motors.velocity.carbookingservice.repository;

import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<CarBooking, UUID> {

    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM CarBooking b
        WHERE b.vehicleId = :vehicleId
          AND b.bookingStatus <> :cancelledStatus
          AND b.rentalStart < :rentalEnd
          AND b.rentalEnd > :rentalStart
        """)
    boolean existsOverlappingBooking(
            @Param("vehicleId") String vehicleId,
            @Param("rentalStart") Instant rentalStart,
            @Param("rentalEnd") Instant rentalEnd,
            @Param("cancelledStatus") BookingStatus cancelledStatus);

    Optional<CarBooking> findByPaymentModeAndPaymentReference(PaymentMode paymentMode, String paymentReference);

    Optional<CarBooking> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT b FROM CarBooking b
        WHERE b.paymentMode = :paymentMode
          AND b.bookingStatus = :status
          AND b.paymentDeadline <= :now
        ORDER BY b.paymentDeadline ASC
        """)
    List<CarBooking> findPendingBankTransferBookingsDueForCancellation(
            @Param("paymentMode") PaymentMode paymentMode,
            @Param("status") BookingStatus status,
            @Param("now") Instant now,
            Pageable pageable);

    @Modifying
    @Query("""
        UPDATE CarBooking b
        SET b.paymentReceivedAmount = COALESCE(b.paymentReceivedAmount, 0) + :paymentAmount,
            b.paymentReceivedAt = :receivedAt,
            b.bookingStatus = CASE
                WHEN COALESCE(b.paymentReceivedAmount, 0) + :paymentAmount >= b.totalAmount
                    THEN :confirmedStatus
                ELSE :pendingPaymentStatus
            END
        WHERE b.bookingId = :bookingId
          AND b.bookingStatus = :pendingPaymentStatus
        """)
    int applyBankTransferPayment(
            @Param("bookingId") UUID bookingId,
            @Param("paymentAmount") BigDecimal paymentAmount,
            @Param("confirmedStatus") BookingStatus confirmedStatus,
            @Param("pendingPaymentStatus") BookingStatus pendingPaymentStatus,
            @Param("receivedAt") Instant receivedAt);

    @Modifying
    @Query("""
        UPDATE CarBooking b
        SET b.bookingStatus = :cancelledStatus
        WHERE b.bookingId = :bookingId
          AND b.bookingStatus = :pendingStatus
          AND b.paymentDeadline <= :now
        """)
    int cancelPendingBankTransfer(
            @Param("bookingId") UUID bookingId,
            @Param("pendingStatus") BookingStatus pendingStatus,
            @Param("cancelledStatus") BookingStatus cancelledStatus,
            @Param("now") Instant now);
}
