package com.motors.velocity.carbookingservice.config;

import com.motors.velocity.carbookingservice.client.payment.ApiClient;
import com.motors.velocity.carbookingservice.client.payment.api.DefaultApi;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CreditCardPaymentClientConfig {

    @Bean
    public ApiClient creditCardPaymentApiClient(
            @Value("${credit-card-payment.base-url}") String baseUrl,
            @Value("${credit-card-payment.timeout.connect}") Duration connectTimeout,
            @Value("${credit-card-payment.timeout.read}") Duration readTimeout) {

        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(connectTimeout).build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        RestClient restClient =
                RestClient.builder().requestFactory(requestFactory).build();

        ApiClient apiClient = new ApiClient(restClient);
        apiClient.setBasePath(baseUrl);

        return apiClient;
    }

    @Bean
    public DefaultApi creditCardPaymentApi(ApiClient apiClient) {
        return new DefaultApi(apiClient);
    }
}
