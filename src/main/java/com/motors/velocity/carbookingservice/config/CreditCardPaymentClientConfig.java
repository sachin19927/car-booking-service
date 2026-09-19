package com.motors.velocity.carbookingservice.config;

import com.motors.velocity.carbookingservice.client.payment.ApiClient;
import com.motors.velocity.carbookingservice.client.payment.api.DefaultApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CreditCardPaymentClientConfig {

    @Bean
    public ApiClient creditCardPaymentApiClient(
            @Value("${credit-card-payment.base-url}") String baseUrl) {

        ApiClient apiClient = new ApiClient();

        apiClient.setBasePath(baseUrl);

        return apiClient;
    }

    @Bean
    public DefaultApi creditCardPaymentApi(ApiClient apiClient) {
        return new DefaultApi(apiClient);
    }
}
