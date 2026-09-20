package com.motors.velocity.carbookingservice.exception;

public class TransientPaymentServiceException extends RuntimeException {

    public TransientPaymentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
