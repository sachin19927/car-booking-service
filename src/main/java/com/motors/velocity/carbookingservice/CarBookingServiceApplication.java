package com.motors.velocity.carbookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CarBookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarBookingServiceApplication.class, args);
    }
}
