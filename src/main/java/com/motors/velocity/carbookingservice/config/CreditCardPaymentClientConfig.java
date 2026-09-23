package com.motors.velocity.carbookingservice.config;

import com.motors.velocity.carbookingservice.client.payment.ApiClient;
import com.motors.velocity.carbookingservice.client.payment.api.DefaultApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@RequiredArgsConstructor
public class CreditCardPaymentClientConfig {

    private final CreditCardPaymentProperties creditCardPaymentProperties;

    @Bean
    public ApiClient creditCardPaymentApiClient() {

        CreditCardPaymentProperties.Timeout timeout = creditCardPaymentProperties.timeout();

        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(timeout.connect()).build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(timeout.read());

        RestClient restClient =
                RestClient.builder().requestFactory(requestFactory).build();

        ApiClient apiClient = new ApiClient(restClient);
        apiClient.setBasePath(creditCardPaymentProperties.baseUrl());

        return apiClient;
    }

    @Bean
    public DefaultApi creditCardPaymentApi(ApiClient apiClient) {
        return new DefaultApi(apiClient);
    }
}
