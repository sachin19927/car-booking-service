package com.motors.velocity.carbookingservice.dto;

import java.math.BigDecimal;

public record BankTransferPaymentEvent(
        String paymentId, String senderAccountNumber, BigDecimal paymentAmount, String transactionDetails) {}
