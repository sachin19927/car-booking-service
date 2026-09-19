package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.client.payment.model.PaymentStatusResponse;

public interface PaymentService {

    PaymentStatusResponse checkPayment(String paymentReference);
}
