package com.motors.velocity.carbookingservice.exception;

import com.motors.velocity.carbookingservice.model.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public sealed class ApiException extends RuntimeException
        permits BusinessValidationException, ResourceNotFoundException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    protected ApiException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
