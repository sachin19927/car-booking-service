package com.motors.velocity.carbookingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.pricing.daily-rate")
public record PricingProperties(BigDecimal compact, BigDecimal sedan, BigDecimal suv, BigDecimal luxury) {}
