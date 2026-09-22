# Car Booking Service

A production-oriented car booking service built with **Java 25** and **Spring Boot 4.1.1**.

The service provides REST APIs for creating and managing car bookings and supports multiple payment modes, including digital wallet, credit card, cash, and bank transfer.

## Features

- REST API for car booking
- Multiple payment modes:
    - Cash
    - Digital Wallet
    - Credit Card
    - Bank Transfer
- Credit-card payment validation through an external API
- Resilience4j retry and timeout handling
- Kafka-based bank-transfer payment processing
- Partial and cumulative bank-transfer payment support
- Duplicate bank-transfer event protection
- Automatic bank-transfer booking cancellation
- Idempotency support using `Idempotency-Key`
- Request validation
- Vehicle booking overlap protection
- PostgreSQL persistence
- Optimistic locking
- Structured JSON logging
- Micrometer metrics
- Prometheus monitoring
- Distributed tracing support
- Global exception handling
- Docker / Docker Compose support
- Unit and integration testing
- Testcontainers support

---

# Technology Stack

| Technology | Version / Usage |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Maven | 3.8+ |
| PostgreSQL | Relational persistence |
| Apache Kafka | Bank-transfer events |
| Spring Data JPA | Persistence |
| Hibernate | ORM |
| Resilience4j | Retry / resilience |
| Micrometer | Metrics |
| Prometheus | Metrics monitoring |
| Zipkin | Distributed tracing |
| Docker | Containerization |
| Testcontainers | Integration testing |
| JUnit 5 | Testing |
| Mockito | Unit testing |

---

# Prerequisites

Install the following before running the application:

- Java 25
- Maven 3.8+
- Docker Desktop
- Docker Compose
- PostgreSQL 15+ if running PostgreSQL separately
- Kafka 3.x+ if running Kafka separately

Verify Java:

```bash
java -version