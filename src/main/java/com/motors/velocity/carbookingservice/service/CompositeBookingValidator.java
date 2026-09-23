package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.dto.BankTransferPaymentEvent;
import com.motors.velocity.carbookingservice.dto.BookingRequest;
import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.exception.InvalidBankTransferPaymentEventException;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CompositeBookingValidator {

    private final BookingValidator bookingValidator;
    private final VehicleService vehicleService;
    private final BookingRepository bookingRepository;

    public void validateBookingRequest(BookingRequest request) {
        validateCustomerName(request.customerName());
        validateVehicleId(request.vehicleId());
        validateRentalPeriod(request.startDate().toInstant(), request.endDate().toInstant());
        validatePaymentDetails(request);
        validateVehicleAvailability(
                request, request.startDate().toInstant(), request.endDate().toInstant());
    }

    public void validateBankTransferPaymentEvent(BankTransferPaymentEvent event) {
        if (event == null) {
            throw new InvalidBankTransferPaymentEventException("Bank transfer payment event must not be empty");
        }

        validatePaymentId(event.paymentId());
        validatePaymentAmount(event.paymentAmount());
        validateTransactionDetails(event.transactionDetails());
        validateSenderAccountNumber(event.senderAccountNumber());
    }

    private void validateCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            throw new BusinessValidationException(ErrorCode.VALIDATION_FAILED, "Customer name must not be blank");
        }

        if (customerName.length() > 50) {
            throw new BusinessValidationException(
                    ErrorCode.VALIDATION_FAILED, "Customer name must not exceed 50 characters");
        }
    }

    private void validateVehicleId(String vehicleId) {
        if (vehicleId == null || vehicleId.isBlank()) {
            throw new BusinessValidationException(ErrorCode.VALIDATION_FAILED, "Vehicle ID must not be blank");
        }
        if (vehicleId.length() > 50) {
            throw new BusinessValidationException(
                    ErrorCode.VALIDATION_FAILED, "Vehicle ID must not exceed 9 characters");
        }

        vehicleService.validateVehicle(vehicleId);
    }

    private void validateRentalPeriod(Instant rentalStart, Instant rentalEnd) {
        bookingValidator.validateRentalPeriod(rentalStart, rentalEnd);
    }

    private void validatePaymentDetails(BookingRequest request) {
        if (request.paymentMethod().requiresPaymentReference()) {
            String paymentRef = request.paymentReference();
            if (paymentRef == null || paymentRef.isBlank()) {
                throw new BusinessValidationException(
                        ErrorCode.PAYMENT_REFERENCE_REQUIRED,
                        "Payment reference is required for " + request.paymentMethod());
            }
            if (paymentRef.length() > 100) {
                throw new BusinessValidationException(
                        ErrorCode.VALIDATION_FAILED, "Payment reference must not exceed 100 characters");
            }
        }
    }

    private void validateVehicleAvailability(BookingRequest request, Instant rentalStart, Instant rentalEnd) {
        boolean alreadyBooked = bookingRepository.existsOverlappingBooking(
                request.vehicleId(), rentalStart, rentalEnd, BookingStatus.CANCELLED);
        if (alreadyBooked) {
            throw new BusinessValidationException(
                    ErrorCode.VEHICLE_UNAVAILABLE,
                    "Vehicle " + request.vehicleId() + " is not available for the requested period");
        }
    }

    private void validatePaymentId(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new InvalidBankTransferPaymentEventException("paymentId is required");
        }
    }

    private void validatePaymentAmount(BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.signum() <= 0) {
            throw new InvalidBankTransferPaymentEventException("paymentAmount must be greater than zero");
        }
    }

    private void validateTransactionDetails(String transactionDetails) {
        if (transactionDetails == null || transactionDetails.isBlank()) {
            throw new InvalidBankTransferPaymentEventException("transactionDetails is required");
        }
    }

    private void validateSenderAccountNumber(String senderAccountNumber) {
        if (senderAccountNumber == null || senderAccountNumber.isBlank()) {
            throw new InvalidBankTransferPaymentEventException("senderAccountNumber is required");
        }
    }
}
