package com.motors.velocity.carbookingservice.exception;

import com.motors.velocity.carbookingservice.model.ErrorCode;
import org.springframework.http.HttpStatus;

public final class BusinessValidationException extends ApiException {
    public BusinessValidationException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.BAD_REQUEST, message);
    }
}
