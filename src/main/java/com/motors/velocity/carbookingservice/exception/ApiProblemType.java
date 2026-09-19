package com.motors.velocity.carbookingservice.exception;

import java.net.URI;

public enum ApiProblemType {
    MALFORMED_REQUEST("malformed-request"),
    VALIDATION_FAILED("validation-failed"),
    INVALID_REQUEST_PARAMETER("invalid-request-parameter"),
    BUSINESS_VALIDATION("business-validation"),
    RESOURCE_NOT_FOUND("resources-not-found"),
    DATA_INTEGRITY_VIOLATION("data-integrity-violation"),
    INTERNAL_SERVER_ERROR("internal-server-error");

    private static final String BASE_URI = "https://api.velocity.motors.local/promblems/";
    private final String path;

    ApiProblemType(String path) {
        this.path = path;
    }

    public URI toUri() {
        return URI.create(BASE_URI + path);
    }
}
