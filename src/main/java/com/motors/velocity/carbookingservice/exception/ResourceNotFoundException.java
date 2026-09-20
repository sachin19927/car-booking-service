package com.motors.velocity.carbookingservice.exception;

import com.motors.velocity.carbookingservice.model.ErrorCode;
import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
    }
}
