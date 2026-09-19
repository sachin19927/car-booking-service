package com.motors.velocity.carbookingservice.repository;

import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
