package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.dto.BookingRequest;
import com.motors.velocity.carbookingservice.dto.BookingResponse;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.mapper.BookingMapper;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BookingService {

    private final VehicleService vehicleService;
    private final BookingValidator bookingValidator;
    private final BookingMapper bookingMapper;
    private final BookingRepository bookingRepository;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {

        validateBusinessRules(request);
        CarBooking carBooking = bookingMapper.toBooking(request);
        bookingRepository.save(carBooking);
        return bookingMapper.toBookingResponse(carBooking);
    }

    private void validateBusinessRules(BookingRequest request) {

        if (request.startDate().isAfter(request.endDate())) {
            throw new BusinessValidationException(ErrorCode.STARTED_AT_REQUIRED, "Start date must be before end date");
        }

        // 1. Validate vehicle
        vehicleService.validateVehicle(request.vehicleId());

        // 2. Convert API ZonedDateTime → Instant
        Instant rentalStart = request.startDate().toInstant();

        Instant rentalEnd = request.endDate().toInstant();

        // 3. Validate rental period
        bookingValidator.validateRentalPeriod(rentalStart, rentalEnd);

        // 4. Check vehicle availability
        boolean alreadyBooked = bookingRepository.existsOverlappingBooking(
                request.vehicleId(), rentalStart, rentalEnd, BookingStatus.CANCELLED);

        if (alreadyBooked) {
            throw new BusinessValidationException(
                    ErrorCode.VEHICLE_UNAVAILABLE,
                    "Vehicle " + request.vehicleId() + " is not available for the requested period");
        }
    }
}
