package com.motors.velocity.carbookingservice.integration.common;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("it")
@DisabledIfSystemProperty(named = "skipTests", matches = "true")
public abstract class AbstractContainerIT {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("velocity_motors_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(false)
            .withStartupTimeout(Duration.ofMinutes(2));

    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"))
            .withReuse(false)
            .withStartupTimeout(Duration.ofMinutes(2));

    static {
        POSTGRES.start();
        KAFKA.start();
        WireMockExtensionHelper.getWireMockExtension().start();
    }

    @DynamicPropertySource
    static void registerDataSources(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl());
        registry.add("spring.datasource.username", () -> POSTGRES.getUsername());
        registry.add("spring.datasource.password", () -> POSTGRES.getPassword());
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("app.kafka.bank-transfer-payment-events.topic", () -> "bank-transfer-payment-events-test");
        registry.add("app.kafka.bank-transfer-payment-events.dlt-topic", () -> "bank-transfer-payment-events-test.DLT");

        registry.add(
                "credit-card-payment.base-url",
                () -> WireMockExtensionHelper.getWireMockBaseUrl() + "/credit-card-payment-api");
    }

    protected void waitForKafkaReady() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
    }

    protected String getKafkaBootstrapServers() {
        return KAFKA.getBootstrapServers();
    }

    protected String getPostgresJdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }
}
