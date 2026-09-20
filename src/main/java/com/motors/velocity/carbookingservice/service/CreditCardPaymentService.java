package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.client.payment.api.DefaultApi;
import com.motors.velocity.carbookingservice.client.payment.model.PaymentStatusResponse;
import com.motors.velocity.carbookingservice.client.payment.model.PaymentStatusRetrievalRequest;
import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.exception.ResourceNotFoundException;
import com.motors.velocity.carbookingservice.exception.TransientPaymentServiceException;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import com.motors.velocity.carbookingservice.model.MetricConstants;
import com.motors.velocity.carbookingservice.observability.BookingMetrics;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCardPaymentService implements PaymentService {

    private final DefaultApi creditCardPaymentApi;
    private final BookingMetrics bookingMetrics;

    @Retry(name = "creditCardPayment")
    @Override
    public PaymentStatusResponse checkPayment(String paymentReference) {

        Timer.Sample timer = bookingMetrics.startCreditCardTimer();
        PaymentStatusRetrievalRequest request = new PaymentStatusRetrievalRequest();

        request.setPaymentReference(paymentReference);

        try {

            PaymentStatusResponse response = creditCardPaymentApi.paymentStatusPost(request);

            // Response validation will be handled here
            if (response == null || response.getStatus() == null) {
                bookingMetrics.recordCreditCardOutcome(MetricConstants.INVALID_RESPONSE.name());
                throw new BusinessValidationException(
                        ErrorCode.INVALID_PAYMENT_SERVICE_RESPONSE,
                        "Payment service returned an invalid payment response");
            }

            if (response.getStatus() != PaymentStatusResponse.StatusEnum.APPROVED) {
                bookingMetrics.recordCreditCardOutcome(MetricConstants.REJECTED.name());
                throw new BusinessValidationException(
                        ErrorCode.PAYMENT_NOT_APPROVED, "Credit card payment was not approved");
            }

            bookingMetrics.recordCreditCardOutcome(MetricConstants.APPROVED.name());
            return response;

        } catch (RestClientResponseException ex) {

            if (ex.getStatusCode().is4xxClientError()) {
                bookingMetrics.recordCreditCardOutcome(MetricConstants.INVALID_REFERENCE.name());
                if (ex.getStatusCode().value() == 400) {
                    throw new BusinessValidationException(
                            ErrorCode.PAYMENT_REFERENCE_INVALID, "Payment reference is invalid");
                }

                if (ex.getStatusCode().value() == 404) {
                    bookingMetrics.recordCreditCardOutcome(MetricConstants.NOT_FOUND.name());
                    throw new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND, "Payment was not found");
                }
            }

            if (ex.getStatusCode().is5xxServerError()) {
                bookingMetrics.recordCreditCardOutcome(MetricConstants.TRANSIENT_FAILURE.name());
                throw new TransientPaymentServiceException("Credit card payment service is unavailable", ex);
            }

            throw ex;
        } catch (BusinessValidationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            bookingMetrics.recordCreditCardOutcome(MetricConstants.TRANSIENT_FAILURE.name());
            log.warn("Credit card validation service call failed with a transient error", ex);
            throw ex;
        } finally {
            bookingMetrics.stopCreditCardTimer(timer);
        }
    }
}
