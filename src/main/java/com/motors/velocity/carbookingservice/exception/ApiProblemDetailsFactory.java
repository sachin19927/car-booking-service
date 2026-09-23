package com.motors.velocity.carbookingservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public final class ApiProblemDetailsFactory {
    private static final String ERROR_CODE = "errorCode";
    private static final String TIME_STAMP = "timeStamp";
    private static final String TRACE_ID = "traceId";
    private static final String FIELD_ERRORS = "fieldErrors";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private ApiProblemDetailsFactory() {}

    public static ProblemDetail create(
            HttpStatus status,
            ApiProblemType type,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request) {
        return create(status, type, title, detail, errorCode, request, null);
    }

    public static ProblemDetail create(
            HttpStatus status,
            ApiProblemType type,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request,
            @Nullable List<ApiFieldError> fieldErrors) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(type.toUri());
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty(ERROR_CODE, errorCode);
        problemDetail.setProperty(TIME_STAMP, Instant.now().toString());
        problemDetail.setProperty(TRACE_ID, resolveTraceId(request));
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            problemDetail.setProperty(FIELD_ERRORS, fieldErrors);
        }
        return problemDetail;
    }

    private static String resolveTraceId(HttpServletRequest request) {

        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }

        Object requestIdAttribute = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (requestIdAttribute instanceof String requestId && !requestId.isBlank()) {
            return requestId;
        }

        String requestIdHeader = request.getHeader(REQUEST_ID_HEADER);
        if (requestIdHeader != null && !requestIdHeader.isBlank()) {
            return requestIdHeader;
        }
        return request.getRequestId();
    }
}
