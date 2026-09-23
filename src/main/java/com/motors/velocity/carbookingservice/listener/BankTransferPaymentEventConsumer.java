package com.motors.velocity.carbookingservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motors.velocity.carbookingservice.config.KafkaProperties;
import com.motors.velocity.carbookingservice.dto.BankTransferPaymentEvent;
import com.motors.velocity.carbookingservice.exception.InvalidBankTransferPaymentEventException;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import com.motors.velocity.carbookingservice.service.BankTransferPaymentService;
import com.motors.velocity.carbookingservice.service.CompositeBookingValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class BankTransferPaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final CompositeBookingValidator compositeBookingValidator;
    private final BankTransferPaymentService paymentService;
    private final BookingMetrics bookingMetrics;
    private final KafkaProperties kafkaProperties;

    @KafkaListener(
            topics = "${app.kafka.bank-transfer-payment-events.topic:bank-transfer-payment-events}",
            groupId = "${app.kafka.consumer.group-id:car-booking-service}",
            containerFactory = "bankTransferKafkaListenerContainerFactory")
    public void consume(String payload) {
        bookingMetrics.recordBankTransferEventReceived();
        try {
            BankTransferPaymentEvent event = parse(payload);
            paymentService.processPaymentEvent(event);
            bookingMetrics.recordBankTransferEventProcessed();
        } catch (InvalidBankTransferPaymentEventException ex) {
            bookingMetrics.recordBankTransferEventFailed("invalid_event");
            log.error(
                    "Invalid bank transfer payment event topic={} payload={}",
                    kafkaProperties.bankTransferPaymentEvents().topic(),
                    payload,
                    ex);
            throw ex;
        } catch (RuntimeException ex) {
            bookingMetrics.recordBankTransferEventFailed("processing_failure");
            log.error(
                    "Failed to process bank transfer payment event topic={} payload={}",
                    kafkaProperties.bankTransferPaymentEvents().topic(),
                    payload,
                    ex);
            throw ex;
        }
    }

    private BankTransferPaymentEvent parse(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new InvalidBankTransferPaymentEventException("Bank transfer payment event must not be empty");
        }
        try {
            BankTransferPaymentEvent event = objectMapper.readValue(payload, BankTransferPaymentEvent.class);
            compositeBookingValidator.validateBankTransferPaymentEvent(event);
            return event;
        } catch (InvalidBankTransferPaymentEventException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidBankTransferPaymentEventException("Unable to deserialize bank transfer payment event", ex);
        }
    }
}
