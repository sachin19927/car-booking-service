package com.motors.velocity.carbookingservice.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pricing.daily-rate")
public record PricingProperties(BigDecimal compact, BigDecimal sedan, BigDecimal suv, BigDecimal luxury) {}
