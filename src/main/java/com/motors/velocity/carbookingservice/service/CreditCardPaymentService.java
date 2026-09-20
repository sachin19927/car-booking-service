package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.client.payment.api.DefaultApi;
import com.motors.velocity.carbookingservice.client.payment.model.PaymentStatusResponse;
import com.motors.velocity.carbookingservice.client.payment.model.PaymentStatusRetrievalRequest;
import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.exception.ResourceNotFoundException;
import com.motors.velocity.carbookingservice.exception.TransientPaymentServiceException;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class CreditCardPaymentService implements PaymentService {

    private final DefaultApi creditCardPaymentApi;

    @Retry(name = "creditCardPayment")
    @Override
    public PaymentStatusResponse checkPayment(String paymentReference) {

        PaymentStatusRetrievalRequest request = new PaymentStatusRetrievalRequest();

        request.setPaymentReference(paymentReference);

        try {

            PaymentStatusResponse response = creditCardPaymentApi.paymentStatusPost(request);

            // Response validation will be handled here
            if (response == null) {
                throw new BusinessValidationException(
                        ErrorCode.INVALID_PAYMENT_SERVICE_RESPONSE, "Payment service returned an empty response");
            }

            if (response.getStatus() == null) {
                throw new BusinessValidationException(
                        ErrorCode.INVALID_PAYMENT_SERVICE_RESPONSE,
                        "Payment service returned an invalid payment status");
            }

            if (response.getStatus() != PaymentStatusResponse.StatusEnum.APPROVED) {
                throw new BusinessValidationException(
                        ErrorCode.PAYMENT_NOT_APPROVED, "Credit card payment was not approved");
            }

            return response;

        } catch (RestClientResponseException ex) {

            if (ex.getStatusCode().is4xxClientError()) {

                if (ex.getStatusCode().value() == 400) {
                    throw new BusinessValidationException(
                            ErrorCode.PAYMENT_REFERENCE_INVALID, "Payment reference is invalid");
                }

                if (ex.getStatusCode().value() == 404) {
                    throw new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND, "Payment was not found");
                }
            }

            if (ex.getStatusCode().is5xxServerError()) {
                throw new TransientPaymentServiceException("Credit card payment service is unavailable", ex);
            }

            throw ex;
        }
    }
}
