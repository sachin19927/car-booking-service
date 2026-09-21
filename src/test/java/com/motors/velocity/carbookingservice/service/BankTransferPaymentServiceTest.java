package com.motors.velocity.carbookingservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.motors.velocity.carbookingservice.dto.BankTransferPaymentEvent;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.model.PaymentMode;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import com.motors.velocity.carbookingservice.repository.BankTransferPaymentEventRepository;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BankTransferPaymentServiceTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final BankTransferPaymentEventRepository eventRepository = mock(BankTransferPaymentEventRepository.class);
    private final BookingMetrics metrics = mock(BookingMetrics.class);
    private final BankTransferPaymentService service =
            new BankTransferPaymentService(bookingRepository, eventRepository, metrics);

    @Test
    void partialPaymentKeepsBookingPending() {
        UUID id = UUID.randomUUID();
        CarBooking booking = booking(id, "100.00", BookingStatus.PENDING_PAYMENT, "TXN123456789");
        CarBooking updated = booking(id, "100.00", BookingStatus.PENDING_PAYMENT, "TXN123456789");
        updated.setPaymentReceivedAmount(new BigDecimal("40.00"));

        when(bookingRepository.findByPaymentModeAndPaymentReference(PaymentMode.BANK_TRANSFER, "TXN123456789"))
                .thenReturn(Optional.of(booking));
        when(eventRepository.insertIfAbsent(any(), eq("PAY-1"), eq(id), eq(new BigDecimal("40.00")), any()))
                .thenReturn(1);
        when(bookingRepository.applyBankTransferPayment(eq(id), eq(new BigDecimal("40.00")), any()))
                .thenReturn(1);
        when(bookingRepository.findById(id)).thenReturn(Optional.of(updated));

        service.processPaymentEvent(
                new BankTransferPaymentEvent("PAY-1", "NL00BANK", new BigDecimal("40.00"), "TXN123456789 " + id));

        verify(metrics).recordBankTransferPartialPayment();
        verify(metrics, never()).recordBankTransferConfirmed();
    }

    @Test
    void cumulativePaymentsConfirmOnlyWhenFullAmountIsReceived() {
        UUID id = UUID.randomUUID();

        CarBooking initialBooking = booking(id, "100.00", BookingStatus.PENDING_PAYMENT, "TXN123456789");

        CarBooking afterFirstPayment = booking(id, "100.00", BookingStatus.PENDING_PAYMENT, "TXN123456789");

        afterFirstPayment.setPaymentReceivedAmount(new BigDecimal("60.00"));

        CarBooking afterSecondPayment = booking(id, "100.00", BookingStatus.CONFIRMED, "TXN123456789");

        afterSecondPayment.setPaymentReceivedAmount(new BigDecimal("100.00"));

        /*
         * Each payment event calls findById() twice:
         *
         * Payment 1:
         *   1. find booking
         *   2. reload after payment
         *
         * Payment 2:
         *   3. find booking
         *   4. reload after payment
         */
        when(bookingRepository.findById(id))
                .thenReturn(
                        Optional.of(initialBooking),
                        Optional.of(afterFirstPayment),
                        Optional.of(afterFirstPayment),
                        Optional.of(afterSecondPayment));

        // First payment: €60
        when(eventRepository.insertIfAbsent(any(), eq("PAY-1"), eq(id), eq(new BigDecimal("60.00")), any()))
                .thenReturn(1);

        when(bookingRepository.applyBankTransferPayment(eq(id), eq(new BigDecimal("60.00")), any()))
                .thenReturn(1);

        // Second payment: €40
        when(eventRepository.insertIfAbsent(any(), eq("PAY-2"), eq(id), eq(new BigDecimal("40.00")), any()))
                .thenReturn(1);

        when(bookingRepository.applyBankTransferPayment(eq(id), eq(new BigDecimal("40.00")), any()))
                .thenReturn(1);

        // -----------------------------------
        // Payment 1: €60
        // -----------------------------------

        service.processPaymentEvent(
                new BankTransferPaymentEvent("PAY-1", "NL00BANK", new BigDecimal("60.00"), "TXN123456789 " + id));

        verify(metrics).recordBankTransferPartialPayment();

        // -----------------------------------
        // Payment 2: €40
        // -----------------------------------

        service.processPaymentEvent(
                new BankTransferPaymentEvent("PAY-2", "NL00BANK", new BigDecimal("40.00"), "TXN123456789 " + id));

        verify(metrics).recordBankTransferConfirmed();

        verify(metrics, never()).recordBankTransferEventIgnored("already_confirmed");
    }

    @Test
    void duplicatePaymentEventIsIgnored() {
        UUID id = UUID.randomUUID();
        CarBooking booking = booking(id, "100.00", BookingStatus.PENDING_PAYMENT, "TXN123456789");
        when(bookingRepository.findByPaymentModeAndPaymentReference(PaymentMode.BANK_TRANSFER, "TXN123456789"))
                .thenReturn(Optional.of(booking));
        when(eventRepository.insertIfAbsent(any(), eq("PAY-3"), eq(id), any(), any()))
                .thenReturn(0);

        service.processPaymentEvent(
                new BankTransferPaymentEvent("PAY-3", "NL00BANK", new BigDecimal("100.00"), "TXN123456789 " + id));

        verify(metrics).recordBankTransferEventIgnored("duplicate_payment_event");
        verify(bookingRepository, never()).applyBankTransferPayment(any(), any(), any());
    }

    private CarBooking booking(UUID id, String total, BookingStatus status, String paymentReference) {
        CarBooking booking = new CarBooking();
        booking.setBookingId(id);
        booking.setPaymentMode(PaymentMode.BANK_TRANSFER);
        booking.setBookingStatus(status);
        booking.setPaymentReference(paymentReference);
        booking.setTotalAmount(new BigDecimal(total));
        booking.setPaymentReceivedAmount(BigDecimal.ZERO);
        return booking;
    }
}
