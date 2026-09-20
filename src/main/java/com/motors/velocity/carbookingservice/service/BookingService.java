package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.dto.BookingRequest;
import com.motors.velocity.carbookingservice.dto.BookingResponse;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.mapper.BookingMapper;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import io.micrometer.core.instrument.Timer;
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
    private final PaymentService paymentService;
    private final BookingMetrics bookingMetrics;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Timer.Sample timer = bookingMetrics.startBookingTimer();
        try {
            validateBusinessRules(request);
            CarBooking carBooking = bookingMapper.toBooking(request);
            processPayment(carBooking);
            bookingRepository.save(carBooking);
            bookingMetrics.recordBookingCreated(carBooking.getPaymentMode(), carBooking.getBookingStatus());
            log.info(
                    "Booking created bookingId={} vehicleId={} paymentMode={} bookingStatus={}",
                    carBooking.getBookingId(),
                    carBooking.getVehicleId(),
                    carBooking.getPaymentMode(),
                    carBooking.getBookingStatus());
            return bookingMapper.toBookingResponse(carBooking);
        } catch (BusinessValidationException ex) {
            bookingMetrics.recordBookingFailure(ex.getErrorCode().name());
            throw ex;
        } catch (RuntimeException ex) {
            bookingMetrics.recordBookingFailure(ErrorCode.INTERNAL_SERVER_ERROR.name());
            throw ex;
        } finally {
            bookingMetrics.stopBookingTimer(timer);
        }
    }

    private void validateBusinessRules(BookingRequest request) {

        // 1. Validate vehicle
        vehicleService.validateVehicle(request.vehicleId());

        // 2. Convert API ZonedDateTime → Instant
        Instant rentalStart = request.startDate().toInstant();
        Instant rentalEnd = request.endDate().toInstant();
        // 3. Validate rental period
        bookingValidator.validateRentalPeriod(rentalStart, rentalEnd);

        validatePaymentDetails(request);

        // 4. Check vehicle availability
        validateAvailability(request, rentalStart, rentalEnd);
    }

    private void processPayment(CarBooking booking) {

        switch (booking.getPaymentMode()) {
            case CASH, DIGITAL_WALLET -> booking.confirm();
            case CREDIT_CARD -> processCreditCardPayment(booking);
            case BANK_TRANSFER -> processBankTransferPayment(booking);
        }
    }

    private void validatePaymentDetails(BookingRequest request) {

        if (request.paymentMethod().requiresPaymentReference()
                && (request.paymentReference() == null
                        || request.paymentReference().isBlank())) {

            throw new BusinessValidationException(
                    ErrorCode.PAYMENT_REFERENCE_REQUIRED,
                    "Payment reference is required for " + request.paymentMethod());
        }
    }

    private void validateAvailability(BookingRequest request, Instant rentalStart, Instant rentalEnd) {
        boolean alreadyBooked = bookingRepository.existsOverlappingBooking(
                request.vehicleId(), rentalStart, rentalEnd, BookingStatus.CANCELLED);

        if (alreadyBooked) {
            throw new BusinessValidationException(
                    ErrorCode.VEHICLE_UNAVAILABLE,
                    "Vehicle " + request.vehicleId() + " is not available for the requested period");
        }
    }

    private void processCreditCardPayment(CarBooking booking) {
        paymentService.checkPayment(booking.getPaymentReference());
        booking.confirm();
    }

    private void processBankTransferPayment(CarBooking booking) {
        booking.paymentPending();
    }
}
