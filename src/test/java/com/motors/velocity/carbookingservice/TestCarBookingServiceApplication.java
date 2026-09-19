package com.motors.velocity.carbookingservice;

import org.springframework.boot.SpringApplication;

public class TestCarBookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(CarBookingServiceApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
