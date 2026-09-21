package com.motors.velocity.carbookingservice.exception;

public class InvalidBankTransferPaymentEventException extends RuntimeException {

    public InvalidBankTransferPaymentEventException(String message) {
        super(message);
    }

    public InvalidBankTransferPaymentEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
