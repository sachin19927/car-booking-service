package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.dto.BookingRequest;
import com.motors.velocity.carbookingservice.dto.BookingResponse;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.mapper.BookingMapper;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BookingCreationService {

    private final CompositeBookingValidator compositeBookingValidator;
    private final BookingMapper bookingMapper;
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final BookingPricingService pricingService;
    private final BookingMetrics bookingMetrics;

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String idempotencyKey, String fingerprint) {
        Timer.Sample timer = bookingMetrics.startBookingTimer();
        try {
            validateBusinessRules(request);
            CarBooking carBooking = prepareBooking(request, idempotencyKey, fingerprint);
            processPayment(carBooking);
            bookingRepository.saveAndFlush(carBooking);
            bookingMetrics.recordBookingCreated(carBooking.getPaymentMode(), carBooking.getBookingStatus());
            log.info(
                    "Booking created bookingId={} vehicleId={} paymentMode={} bookingStatus={} totalAmount={}",
                    carBooking.getBookingId(),
                    carBooking.getVehicleId(),
                    carBooking.getPaymentMode(),
                    carBooking.getBookingStatus(),
                    carBooking.getTotalAmount());
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
        compositeBookingValidator.validateBookingRequest(request);
    }

    private @NonNull CarBooking prepareBooking(BookingRequest request, String idempotencyKey, String fingerprint) {
        CarBooking carBooking = bookingMapper.toBooking(request);
        carBooking.setTotalAmount(pricingService.calculateTotalAmount(carBooking));
        carBooking.setIdempotencyKey(idempotencyKey);
        carBooking.setRequestFingerprint(fingerprint);
        return carBooking;
    }

    private void processPayment(CarBooking booking) {
        switch (booking.getPaymentMode()) {
            case CASH, DIGITAL_WALLET -> booking.confirm();
            case CREDIT_CARD -> processCreditCardPayment(booking);
            case BANK_TRANSFER -> processBankTransferPayment(booking);
        }
    }

    private void processCreditCardPayment(CarBooking booking) {
        paymentService.checkPayment(booking.getPaymentReference());
        booking.confirm();
    }

    private void processBankTransferPayment(CarBooking booking) {
        booking.paymentPending();
        booking.setPaymentDeadlineFromRentalStart();
        booking.setPaymentReceivedAmount(BigDecimal.ZERO.setScale(2));
    }
}
