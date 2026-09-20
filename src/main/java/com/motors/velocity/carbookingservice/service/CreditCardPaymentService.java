package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.client.payment.api.DefaultApi;
import com.motors.velocity.carbookingservice.client.payment.model.PaymentStatusResponse;
import com.motors.velocity.carbookingservice.client.payment.model.PaymentStatusRetrievalRequest;
import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreditCardPaymentService implements PaymentService {

    private final DefaultApi creditCardPaymentApi;

    @Override
    public PaymentStatusResponse checkPayment(String paymentReference) {
        PaymentStatusRetrievalRequest request = new PaymentStatusRetrievalRequest();

        request.setPaymentReference(paymentReference);

        PaymentStatusResponse response = creditCardPaymentApi.paymentStatusPost(request);

        if (response.getStatus() != PaymentStatusResponse.StatusEnum.APPROVED) {

            throw new BusinessValidationException(
                    ErrorCode.PAYMENT_NOT_APPROVED, "Credit card payment was not approved");
        }

        return response;
    }
}
