package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.client.payment.api.DefaultApi;
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
    private final DefaultApi creditCardPaymentApi;
    private final PaymentService paymentService;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {

        validateBusinessRules(request);
        CarBooking carBooking = bookingMapper.toBooking(request);
        processPayment(carBooking);
        bookingRepository.save(carBooking);
        return bookingMapper.toBookingResponse(carBooking);
    }

    private void validateBusinessRules(BookingRequest request) {

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

    private void processPayment(CarBooking booking) {

        switch (booking.getPaymentMode()) {
            case CASH -> booking.confirm();

            case CREDIT_CARD -> processCreditCardPayment(booking);

            case BANK_TRANSFER -> {
                // Remain PENDING_PAYMENT
            }
        }
    }

    private void processCreditCardPayment(CarBooking booking) {

        paymentService.checkPayment(booking.getPaymentReference());

        booking.confirm();
    }
}
