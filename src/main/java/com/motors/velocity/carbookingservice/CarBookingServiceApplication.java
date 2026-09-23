package com.motors.velocity.carbookingservice;

import com.motors.velocity.carbookingservice.config.BookingCancellationProperties;
import com.motors.velocity.carbookingservice.config.CreditCardPaymentProperties;
import com.motors.velocity.carbookingservice.config.KafkaProperties;
import com.motors.velocity.carbookingservice.config.PricingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({
    PricingProperties.class,
    KafkaProperties.class,
    CreditCardPaymentProperties.class,
    BookingCancellationProperties.class
})
public class CarBookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarBookingServiceApplication.class, args);
    }
}
