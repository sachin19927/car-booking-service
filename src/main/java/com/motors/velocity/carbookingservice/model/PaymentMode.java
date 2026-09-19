package com.motors.velocity.carbookingservice.model;

public enum PaymentMode {
    CASH(false),
    CREDIT_CARD(true),
    BANK_TRANSFER(true);

    private final boolean paymentReferenceRequired;

    PaymentMode(boolean paymentReferenceRequired) {
        this.paymentReferenceRequired = paymentReferenceRequired;
    }

    public boolean requiresPaymentReference() {
        return paymentReferenceRequired;
    }
}
