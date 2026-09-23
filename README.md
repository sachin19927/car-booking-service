# Car Booking Service

A production-oriented car booking service built with **Java 25** and **Spring Boot 4.1.1**.

The service provides REST APIs for creating and retrieving car bookings and supports multiple payment modes, including:

* Cash
* Digital Wallet
* Credit Card
* Bank Transfer

The implementation includes persistence, validation, payment integration, asynchronous Kafka processing, idempotency, retry handling, automatic cancellation, database constraints, observability, structured logging, tracing, Docker Compose infrastructure, unit tests, integration tests, and Postman acceptance testing.

---

# 1. Features

## Booking

* Create a car booking.
* Retrieve a booking by booking ID.
* Vehicle validation against the configured mock vehicle catalogue.
* Vehicle availability/overlap validation.
* Rental-period validation.
* Maximum rental period of 21 days.
* Configurable daily pricing.
* PostgreSQL persistence.
* Database-level booking invariants.
* Optimistic locking support.
* Automatic timestamps.

## Payment Modes

### Cash

Cash bookings are confirmed immediately.

```text
Booking Request
      |
      v
Validation
      |
      v
Pricing
      |
      v
CASH
      |
      v
CONFIRMED
```

### Digital Wallet

Digital-wallet bookings are confirmed immediately.

```text
Booking Request
      |
      v
Validation
      |
      v
Pricing
      |
      v
DIGITAL_WALLET
      |
      v
CONFIRMED
```

### Credit Card

Credit-card payments are validated through an external HTTP API.

The service uses:

* OpenAPI-generated Java client
* Configurable connect/read timeout
* Resilience4j retry
* Exponential backoff
* Randomized wait
* Payment outcome metrics
* Payment latency metrics
* Retry metrics
* Docker WireMock for integration testing

```text
Booking API
     |
     v
CreditCardPaymentService
     |
     v
Generated OpenAPI Client
     |
     v
Credit Card Payment API
     |
     +---- APPROVED ----> CONFIRMED
     |
     +---- REJECTED ----> Business Error
     |
     +---- 404 ----------> PAYMENT_NOT_FOUND
     |
     +---- transient failure
              |
              v
           Retry
              |
              +---- success ----> CONFIRMED
              |
              +---- exhausted --> Error
```

### Bank Transfer

Bank-transfer payments are asynchronous.

A booking initially enters:

```text
PENDING_PAYMENT
```

The payment event is published to Kafka.

```text
Bank Webhook / Producer
          |
          v
       Kafka
          |
          v
BankTransferPaymentEventConsumer
          |
          v
BankTransferPaymentService
          |
          v
      PostgreSQL
          |
          +---- partial payment --> PENDING_PAYMENT
          |
          +---- full payment ----> CONFIRMED
```

The implementation supports:

* Partial payments
* Cumulative payments
* Duplicate payment-event protection
* Payment-event persistence
* Unknown booking handling
* Malformed event handling
* Kafka retry
* Dead Letter Topic
* Automatic cancellation after the payment deadline

---

# 2. Technology Stack

| Technology               | Version / Usage                     |
| ------------------------ | ----------------------------------- |
| Java                     | 25                                  |
| Spring Boot              | 4.1.1                               |
| Maven                    | 3.8+                                |
| PostgreSQL               | 18 Alpine in Docker Compose         |
| Apache Kafka             | 4.0.0 in Docker Compose             |
| Spring Data JPA          | Persistence                         |
| Hibernate                | ORM                                 |
| Flyway                   | Database migrations                 |
| Resilience4j             | Retry / resilience                  |
| Micrometer               | Application metrics                 |
| Prometheus               | Metrics endpoint                    |
| Zipkin                   | Distributed tracing                 |
| Docker                   | Containerization                    |
| Docker Compose           | Local infrastructure                |
| Testcontainers           | Integration testing                 |
| JUnit 5                  | Automated testing                   |
| Mockito                  | Unit testing                        |
| Awaitility               | Asynchronous integration assertions |
| WireMock                 | External payment-service simulation |
| OpenAPI Generator        | Credit-card client generation       |
| MapStruct                | DTO/entity mapping                  |
| Logstash Logback Encoder | Structured JSON logging             |

---

# 3. High-Level Architecture

```text
                         +----------------------+
                         |      REST Client      |
                         | Postman / Frontend    |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         |   BookingController  |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         |    BookingService    |
                         +----------+-----------+
                                    |
                    +---------------+----------------+
                    |               |                |
                    v               v                v
             Validation         Pricing           Idempotency
                    |               |                |
                    +---------------+----------------+
                                    |
                                    v
                         +----------------------+
                         | BookingCreationSvc   |
                         +----------+-----------+
                                    |
                 +----------------+------------------+
                 |                |                  |
                 v                v                  v
               CASH        DIGITAL_WALLET      CREDIT_CARD
                 |                |                  |
                 |                |                  v
                 |                |        External Payment API
                 |                |                  |
                 +----------------+------------------+
                                    |
                                    v
                              PostgreSQL
```

Bank-transfer processing is asynchronous:

```text
                 Bank Transfer Event
                         |
                         v
                      Kafka
                         |
                         v
          BankTransferPaymentEventConsumer
                         |
                         v
              BankTransferPaymentService
                         |
             +-----------+-----------+
             |                       |
             v                       v
        PostgreSQL                 Failure
                                     |
                                     v
                                  Retry
                                     |
                                     v
                                    DLT
```

---

# 4. Project Structure

```text
car-booking-service/
│
├── src/
│   ├── main/
│   │   ├── java/com/motors/velocity/carbookingservice/
│   │   │
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── logback-spring.xml
│   │       │
│   │       ├── db/migration/
│   │       │   ├── V1__create_car_booking.sql
│   │       │   ├── V2__add_car_booking_indexes.sql
│   │       │   ├── V3__allow_digital_wallet_payment_mode.sql
│   │       │   ├── V4__add_bank_transfer_payment_tracking.sql
│   │       │   ├── V5__add_booking_amount_and_idempotency.sql
│   │       │   ├── V6__enforce_booking_invariants.sql
│   │       │   └── V7__create_bank_transfer_payment_event.sql
│   │       │
│   │       ├── openapi/
│   │       │   └── credit-card-payment-api.yaml
│   │       │
│   │       └── static/mock/
│   │           └── vehicles.json
│   │
│   └── test/
│       ├── java/
│       │   └── com/motors/velocity/carbookingservice/
│       │       ├── integration/
│       │       └── service/
│       │
│       └── resources/
│           ├── application-it.yml
│           └── application-test.yml
│
├── postman/
│   ├── Car-Booking-Service.postman_collection.json
│   └── Car-Booking-Service.postman_environment.json
│
├── compose.yaml
├── Dockerfile
├── pom.xml
├── .env.example
├── mvnw
└── README.md
```

---

# 5. Main Application Components

## Controllers

### `BookingController`

Base path:

```text
/v1/bookings
```

Endpoints:

```http
POST /v1/bookings
GET  /v1/bookings/{bookingId}
```

### `TestSupportController`

Base path:

```text
/test-support/v1
```

The controller is enabled only when:

```yaml
app:
  test-support:
    enabled: true
```

Endpoints:

```http
POST /test-support/v1/bank-transfer/payment-events

POST /test-support/v1/bank-transfer/raw-events/{key}

GET /test-support/v1/bank-transfer/dlt-events

POST /test-support/v1/cancellations/run
```

These endpoints exist specifically for integration/acceptance testing.

They should remain disabled in normal production deployments.

---

# 6. Booking API

## Create Booking

```http
POST /v1/bookings
Content-Type: application/json
```

Optional header:

```http
Idempotency-Key: <unique-key>
```

Example:

```json
{
  "customerName": "John Doe",
  "vehicleId": "VH-NL-347",
  "startDate": "2035-01-01T10:00:00Z",
  "endDate": "2035-01-03T10:00:00Z",
  "vehicleCategory": "COMPACT",
  "paymentMethod": "CASH"
}
```

Successful response:

```http
201 Created
```

```json
{
  "bookingId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "status": "CONFIRMED"
}
```

---

# 7. Booking Request Fields

| Field              |    Required | Description                                      |
| ------------------ | ----------: | ------------------------------------------------ |
| `customerName`     |         Yes | Maximum 50 characters                            |
| `vehicleId`        |         Yes | Must match a valid vehicle                       |
| `startDate`        |         Yes | Rental start                                     |
| `endDate`          |         Yes | Rental end                                       |
| `vehicleCategory`  |         Yes | COMPACT, SEDAN, SUV, LUXURY                      |
| `paymentMethod`    |         Yes | CASH, DIGITAL_WALLET, CREDIT_CARD, BANK_TRANSFER |
| `paymentReference` | Conditional | Required for CREDIT_CARD and BANK_TRANSFER       |

Vehicle IDs are loaded from:

```text
src/main/resources/static/mock/vehicles.json
```

Current configured vehicles include:

```text
VH-NL-347
VH-NL-821
VH-NL-594
056-NL-JX
721-NL-KR
438-NL-PT
NL-678-AM
NL-214-RK
NL-905-ZT
NL-431-HM
```

---

# 8. Payment Modes

The supported payment modes are:

```text
CASH
DIGITAL_WALLET
CREDIT_CARD
BANK_TRANSFER
```

Payment references are required for:

```text
CREDIT_CARD
BANK_TRANSFER
```

They are not required for:

```text
CASH
DIGITAL_WALLET
```

---

# 9. Booking States

The service uses:

```text
PENDING_PAYMENT
CONFIRMED
CANCELLED
```

## CASH

```text
CASH
  |
  v
CONFIRMED
```

## DIGITAL_WALLET

```text
DIGITAL_WALLET
       |
       v
CONFIRMED
```

## CREDIT_CARD

```text
CREDIT_CARD
     |
     v
External payment validation
     |
     +---- APPROVED ----> CONFIRMED
     |
     +---- failure -----> Error
```

## BANK_TRANSFER

```text
BANK_TRANSFER
      |
      v
PENDING_PAYMENT
      |
      +---- partial payment ----> PENDING_PAYMENT
      |
      +---- full payment -------> CONFIRMED
      |
      +---- deadline expires ---> CANCELLED
```

---

# 10. Pricing

Pricing is configurable.

Default daily rates are:

| Category | Daily Rate |
| -------- | ---------: |
| COMPACT  |        €50 |
| SEDAN    |        €70 |
| SUV      |        €90 |
| LUXURY   |       €120 |

Configuration:

```yaml
app:
  pricing:
    daily-rate:
      compact: 50.00
      sedan: 70.00
      suv: 90.00
      luxury: 120.00
```

Environment variables:

```text
PRICE_COMPACT_PER_DAY
PRICE_SEDAN_PER_DAY
PRICE_SUV_PER_DAY
PRICE_LUXURY_PER_DAY
```

Pricing is calculated using rental duration rounded up to a complete rental day.

Examples:

```text
24 hours      -> 1 day
48 hours      -> 2 days
49 hours      -> 3 days
```

The implementation guarantees at least one rental day.

---

# 11. Rental Validation

The booking validator enforces:

## Start date

The rental start must not be in the past.

## Start/end relationship

```text
start < end
```

is required.

These are invalid:

```text
start > end
start == end
```

## Maximum rental duration

Maximum:

```text
21 days
```

Example:

```text
21 days exactly -> accepted
21 days + 1 sec -> rejected
```

---

# 12. Vehicle Availability

A vehicle cannot have overlapping active bookings.

Cancelled bookings release the vehicle.

The database uses a PostgreSQL exclusion constraint:

```text
vehicle_id + tstzrange(rental_start, rental_end)
```

with overlapping ranges rejected for non-cancelled bookings.

This provides database-level protection in addition to application-level availability validation.

The implementation therefore protects against concurrent booking attempts more robustly than an application-only `exists` check.

---

# 13. Idempotency

Booking creation supports:

```http
Idempotency-Key
```

The key is normalized by trimming whitespace.

Maximum length:

```text
100 characters
```

The request fingerprint is stored with the booking.

## Same key + same request

The existing booking is returned.

```text
Request 1
   |
   v
Create booking

Request 2
same Idempotency-Key
same request
   |
   v
Existing booking replayed
```

## Same key + different request

The request is rejected.

Error:

```text
IDEMPOTENCY_KEY_REUSED
```

## Key too long

The request is rejected.

Error:

```text
INVALID_IDEMPOTENCY_KEY
```

The database also contains a unique index on the idempotency key.

---

# 14. Credit Card Integration

The credit-card API contract is defined in:

```text
src/main/resources/openapi/credit-card-payment-api.yaml
```

The Java client is generated during Maven's `generate-sources` phase using OpenAPI Generator.

The generated client uses:

```text
RestClient
```

and is configured through:

```text
CreditCardPaymentClientConfig
CreditCardPaymentProperties
CreditCardPaymentService
```

Default endpoint:

```text
http://localhost:9090/credit-card-payment-api
```

Configuration:

```yaml
credit-card-payment:
  base-url: http://localhost:9090/credit-card-payment-api
  timeout:
    connect: 2s
    read: 3s
```

Environment variables:

```text
CREDIT_CARD_PAYMENT_BASE_URL
CREDIT_CARD_PAYMENT_CONNECT_TIMEOUT
CREDIT_CARD_PAYMENT_READ_TIMEOUT
```

---

# 15. Credit Card Retry

Resilience4j is configured with:

```yaml
resilience4j:
  retry:
    instances:
      creditCardPayment:
        max-attempts: 3
```

Production defaults include:

```text
max attempts: 3
initial wait: 1 second
exponential backoff: enabled
multiplier: 2
randomized wait: enabled
```

Retryable exceptions include:

```text
TransientPaymentServiceException
ResourceAccessException
```

The integration-test configuration reduces the retry delay so the tests complete faster.

---

# 16. WireMock

Docker Compose starts WireMock at:

```text
http://localhost:9090
```

Container port:

```text
8080
```

Host mapping:

```text
9090:8080
```

The application therefore calls:

```text
http://localhost:9090/credit-card-payment-api
```

WireMock is used to reproduce:

* APPROVED
* REJECTED
* 404
* 400
* 500
* transient 500 followed by success
* slow responses/timeouts
* invalid response payloads

WireMock mappings are mounted from:

```text
./wiremock
```

---

# 17. Bank Transfer Processing

Bank-transfer events use Kafka.

Default topic:

```text
bank-transfer-payment-events
```

Default DLT:

```text
bank-transfer-payment-events.DLT
```

Integration-test topic:

```text
bank-transfer-payment-events-test
```

Integration-test DLT:

```text
bank-transfer-payment-events-test.DLT
```

---

# 18. Bank Transfer Event

The event contains:

```json
{
  "paymentId": "PAY-123",
  "senderAccountNumber": "NL00BANK123",
  "paymentAmount": 50.00,
  "transactionDetails": "TXN123456789 <booking-id>"
}
```

The transaction details must follow the configured format expected by the payment service.

The implementation extracts:

```text
transaction reference
booking identifier
```

from the transaction details.

The service first attempts to resolve the booking by booking ID and then falls back to the bank-transfer payment reference/payment ID.

---

# 19. Partial and Cumulative Payments

Suppose:

```text
Booking total = €140
```

First payment:

```text
€50
```

Result:

```text
received = €50
status   = PENDING_PAYMENT
```

Second payment:

```text
€90
```

Result:

```text
received = €140
status   = CONFIRMED
```

The update is performed atomically in PostgreSQL.

The booking only becomes confirmed once:

```text
paymentReceivedAmount >= totalAmount
```

---

# 20. Duplicate Bank Transfer Events

Every payment event contains a unique:

```text
paymentId
```

The database contains:

```text
UNIQUE(payment_id)
```

The service uses:

```text
INSERT ... ON CONFLICT (payment_id) DO NOTHING
```

Therefore:

```text
Event #1
paymentId = PAY-123
     |
     v
Inserted
     |
     v
Payment applied

Event #2
paymentId = PAY-123
     |
     v
Duplicate detected
     |
     v
Ignored
```

The duplicate event cannot double-credit the booking.

---

# 21. Kafka Retry and DLT

Kafka processing uses:

```text
DefaultErrorHandler
DeadLetterPublishingRecoverer
ExponentialBackOffWithMaxRetries
```

Default configuration:

```text
max attempts = 3
initial backoff = 1 second
multiplier = 2
maximum interval = 30 seconds
```

Certain invalid bank-transfer events are marked non-retryable.

For example:

```text
InvalidBankTransferPaymentEventException
```

is configured as non-retryable and is routed to the DLT.

General processing failures can follow the configured retry path before DLT publication.

---

# 22. Dead Letter Topic

The DLT is:

```text
bank-transfer-payment-events.DLT
```

The Test Support API provides:

```http
GET /test-support/v1/bank-transfer/dlt-events
```

Example:

```http
GET /test-support/v1/bank-transfer/dlt-events?containing=PAY-UNKNOWN
```

This allows Postman or integration tests to verify DLT routing without requiring a separate Kafka client.

---

# 23. Automatic Bank Transfer Cancellation

Bank-transfer bookings receive a payment deadline:

```text
rentalStart - 48 hours
```

Example:

```text
Rental start:
2026-10-10 10:00

Payment deadline:
2026-10-08 10:00
```

Pending bookings whose deadline has passed are cancelled.

The scheduled cancellation configuration is:

```yaml
app:
  booking:
    cancellation:
      fixed-delay: 60000
      batch-size: 500
```

The service processes pending bank-transfer bookings in batches.

The cancellation update is guarded by:

```text
bookingStatus = PENDING_PAYMENT
paymentDeadline <= now
```

so an already confirmed/cancelled booking is not changed.

---

# 24. Test Support API

Test support is disabled by default:

```yaml
app:
  test-support:
    enabled: false
```

Enable it for integration/acceptance testing:

```text
TEST_SUPPORT_ENABLED=true
```

Available endpoints:

## Publish valid payment event

```http
POST /test-support/v1/bank-transfer/payment-events
```

Publishes a properly serialized `BankTransferPaymentEvent` to the configured Kafka topic.

## Publish raw Kafka event

```http
POST /test-support/v1/bank-transfer/raw-events/{key}
```

Publishes an arbitrary raw payload.

This is useful for testing malformed messages.

## Peek DLT

```http
GET /test-support/v1/bank-transfer/dlt-events?containing=<marker>
```

Searches the DLT for a matching record.

## Run cancellation batch

```http
POST /test-support/v1/cancellations/run
```

Runs the same cancellation logic used by the scheduled process without waiting for the scheduler.

---

# 25. Database

PostgreSQL is used for transactional persistence.

Main table:

```text
car_booking
```

Important fields include:

```text
booking_id
customer_name
vehicle_id
rental_start
rental_end
vehicle_category
payment_mode
payment_reference
booking_status
time_zone
created_at
updated_at
payment_deadline
total_amount
payment_received_amount
payment_received_at
idempotency_key
request_fingerprint
```

Bank-transfer events are stored in:

```text
bank_transfer_payment_event
```

---

# 26. Database Migrations

Flyway migrations:

```text
V1__create_car_booking.sql
V2__add_car_booking_indexes.sql
V3__allow_digital_wallet_payment_mode.sql
V4__add_bank_transfer_payment_tracking.sql
V5__add_booking_amount_and_idempotency.sql
V6__enforce_booking_invariants.sql
V7__create_bank_transfer_payment_event.sql
```

Important database protections include:

* rental period check
* vehicle category check
* payment mode check
* booking status check
* positive total amount
* non-negative received payment
* unique payment references where applicable
* unique idempotency key
* unique bank-transfer payment ID
* foreign key from payment event to booking
* PostgreSQL vehicle/rental overlap exclusion constraint

---

# 27. Docker Compose

The project provides:

```text
compose.yaml
```

Services:

```text
postgres
kafka
credit-card-payment-mock
zipkin
```

## PostgreSQL

Host:

```text
localhost:35432
```

Database:

```text
velocity-motors-carrental
```

Default Compose credentials are defined in `compose.yaml` and should be overridden for non-local environments.

## Kafka

Host:

```text
localhost:9092
```

The Compose configuration uses a single Kafka node operating as both broker and controller.

## WireMock

Host:

```text
localhost:9090
```

## Zipkin

Host:

```text
localhost:9411
```

---

# 28. Start Infrastructure

Start all infrastructure:

```bash
docker compose up -d
```

Check containers:

```bash
docker compose ps
```

Check logs:

```bash
docker compose logs -f
```

Stop:

```bash
docker compose down
```

Remove persistent volumes:

```bash
docker compose down -v
```

Use `down -v` carefully because it removes local PostgreSQL/Kafka data.

---

# 29. Environment Configuration

Example environment file:

```text
.env.example
```

Important variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD

KAFKA_BOOTSTRAP_SERVERS
KAFKA_CONSUMER_GROUP_ID
BANK_TRANSFER_PAYMENT_EVENTS_TOPIC
BANK_TRANSFER_PAYMENT_EVENTS_DLT_TOPIC

CREDIT_CARD_PAYMENT_BASE_URL

TEST_SUPPORT_ENABLED

ZIPKIN_ENDPOINT
TRACING_SAMPLING_PROBABILITY

PRICE_COMPACT_PER_DAY
PRICE_SEDAN_PER_DAY
PRICE_SUV_PER_DAY
PRICE_LUXURY_PER_DAY
```

---

# 30. Run the Application

Using Maven wrapper:

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
./mvnw spring-boot:run
```

Or:

```bash
mvn spring-boot:run
```

The application uses:

```text
http://localhost:8080
```

Management/Actuator runs on:

```text
http://localhost:8081
```

---

# 31. Build

```bash
./mvnw clean package
```

Windows:

```powershell
.\mvnw.cmd clean package
```

The generated application JAR is placed under:

```text
target/
```

---

# 32. Unit Tests

Run the unit tests:

```bash
./mvnw test
```

The project contains unit tests for:

```text
BankTransferPaymentService
BookingPricingService
BookingRequestFingerprintService
BookingServiceIdempotency
BookingValidator
```

Examples of unit-test scenarios include:

* partial payment
* cumulative full payment
* duplicate payment
* pricing for exact 48 hours
* pricing for partial rental days
* identical request fingerprints
* different request fingerprints
* idempotent replay
* idempotency-key reuse
* rental-period validation
* maximum rental duration

---

# 33. Integration Tests

Integration tests use Testcontainers and real infrastructure.

The integration-test classes include:

```text
BookingApiIT
BookingCompleteWorkflowIT
CreditCardPaymentServiceIT
BankTransferPaymentEventConsumerIT
```

Infrastructure used by the integration tests includes:

```text
PostgreSQL
Kafka
WireMock
Spring Boot application context
```

---

# 34. Integration Test Configuration

Integration configuration is in:

```text
src/test/resources/application-it.yml
```

It enables:

```yaml
app:
  test-support:
    enabled: true
```

Integration Kafka:

```text
bank-transfer-payment-events-test
```

Integration DLT:

```text
bank-transfer-payment-events-test.DLT
```

Retry delays are reduced for faster test execution.

---

# 35. Running Integration Tests

The Maven Failsafe plugin detects:

```text
**/*IT.*
```

Integration tests are configured with a five-minute timeout.

Run the complete verification lifecycle with integration tests enabled:

```bash
./mvnw verify -DskipITs=false
```

Windows:

```powershell
.\mvnw.cmd verify -DskipITs=false
```

Normal `verify` configuration can skip integration tests unless explicitly enabled through the configured `skipITs` property.

---

# 36. Integration Test Coverage

## `BookingApiIT`

Covers:

* cash booking
* persisted amount
* bank-transfer pending state
* payment deadline
* cumulative payment
* duplicate payment
* automatic cancellation
* idempotency replay
* idempotency-key reuse
* credit-card approved
* credit-card rejected
* invalid rental period
* rental >21 days
* concurrent vehicle booking
* overlapping vehicle booking

---

## `BookingCompleteWorkflowIT`

Covers complete workflows:

```text
Digital Wallet
Credit Card + real WireMock
Bank Transfer + real Kafka consumer
Duplicate Kafka event
```

The bank-transfer test uses the real asynchronous Kafka path.

---

## `CreditCardPaymentServiceIT`

Covers:

```text
APPROVED
REJECTED
404
transient 500 -> success
persistent 500
read timeout
retry exhaustion
```

This test class validates the actual HTTP integration against Docker WireMock rather than mocking the generated payment client.

---

## `BankTransferPaymentEventConsumerIT`

Covers:

```text
valid payment
partial payment
duplicate event
malformed JSON
unknown booking reference
DLT routing
```

---

# 37. Postman Testing

The project contains the original Postman collection:

```text
postman/Car-Booking-Service.postman_collection.json
```

and environment:

```text
postman/Car-Booking-Service.postman_environment.json
```

The expanded acceptance suite additionally covers:

```text
Cash
Digital Wallet
Credit Card
Bank Transfer
Idempotency
Validation
Vehicle overlap
Cancellation
Kafka
DLT
Actuator
Prometheus
```

---

# 38. Postman Test Matrix

## Positive booking flows

```text
Cash -> CONFIRMED

Digital Wallet -> CONFIRMED

Credit Card APPROVED -> CONFIRMED
```

## Credit card failures

```text
REJECTED
404
400
500
Transient failure -> retry -> success
Persistent failure -> retry exhaustion
Invalid response
Read timeout
```

## Bank transfer

```text
PENDING_PAYMENT
Partial payment
Full cumulative payment
Duplicate event
```

## DLT

```text
Malformed JSON
Unknown booking
Invalid event fields
```

## Idempotency

```text
Same key + same request
Same key + different request
Key >100 characters
```

## Validation

```text
Invalid vehicle
Invalid vehicle format
Missing customer
Customer name >50
Missing start date
Missing end date
Missing vehicle category
Missing payment method
Missing payment reference
Payment reference >100
Past start date
End before start
Equal start/end
More than 21 days
Exactly 21 days
Malformed JSON
```

## Vehicle availability

```text
Overlapping booking
Adjacent booking
```

## Cancellation

```text
Create overdue bank-transfer booking
Run cancellation batch
Verify CANCELLED
Verify vehicle becomes available
```

---

# 39. Observability

The service exposes Spring Boot Actuator on:

```text
http://localhost:8081
```

Health:

```http
GET /actuator/health
```

Metrics:

```http
GET /actuator/metrics
```

Prometheus:

```http
GET /actuator/prometheus
```

---

# 40. Application Metrics

`BookingMetrics` records metrics for:

## Booking creation

```text
booking.created.total
booking.failure.total
booking.creation.duration
```

Dimensions include payment mode, booking status, and error type where applicable.

## Credit card

```text
credit_card.payment.total
credit_card.payment.duration
credit_card.payment.retry.total
credit_card.payment.retry.exhausted.total
credit_card.payment.retry.success.total
```

## Bank transfer

```text
bank_transfer.payment_event.total
bank_transfer.payment_event.failure.total
bank_transfer.payment_event.ignored.total
```

Outcomes include events such as:

```text
received
processed
confirmed
partial_payment
duplicate_payment_event
booking_cancelled
already_confirmed
```

## Cancellation

```text
booking.cancellation.total
```

## Idempotency

```text
booking.idempotency.replay.total
```

---

# 41. Prometheus

Prometheus-format metrics are available at:

```text
http://localhost:8081/actuator/prometheus
```

The service enables histogram publishing for:

```text
booking.creation.duration
credit_card.payment.duration
```

These metrics can be consumed by a Prometheus server and visualized in a monitoring dashboard.

---

# 42. Distributed Tracing

The application uses Spring Boot tracing with Zipkin.

Default Zipkin endpoint:

```text
http://localhost:9411/api/v2/spans
```

Zipkin UI:

```text
http://localhost:9411
```

Sampling is configurable:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1
```

For integration tests tracing is disabled:

```yaml
management:
  tracing:
    sampling:
      probability: 0
```

---

# 43. Request Correlation

`RequestTraceFilter` manages:

```text
X-Request-Id
```

Behavior:

```text
Client sends X-Request-Id
        |
        v
Reuse request ID
```

If the client does not provide one:

```text
No X-Request-Id
        |
        v
Generate UUID
```

The request ID is:

* added to the response
* stored in MDC
* available to structured logging

Example:

```http
X-Request-Id: 8b1c7c8e-...
```

---

# 44. Structured Logging

The project uses:

```text
logback-spring.xml
```

with structured JSON logging.

Important contextual information includes:

```text
timestamp
application
traceId
spanId
requestId
level
logger
thread
message
exception
```

This makes logs suitable for centralized log platforms such as ELK/OpenSearch-style systems.

---

# 45. Error Handling

The project uses:

```text
GlobalExceptionHandler
ApiProblemDetailsFactory
ApiException
BusinessValidationException
ResourceNotFoundException
TransientPaymentServiceException
InvalidBankTransferPaymentEventException
```

Business errors use explicit error codes.

Examples:

```text
MAX_RENTAL_DAYS_EXCEEDED
INVALID_RENTAL_PERIOD
VEHICLE_NOT_FOUND
VEHICLE_UNAVAILABLE
PAYMENT_REFERENCE_REQUIRED
PAYMENT_NOT_APPROVED
PAYMENT_REFERENCE_INVALID
PAYMENT_NOT_FOUND
PAYMENT_SERVICE_UNAVAILABLE
INVALID_PAYMENT_SERVICE_RESPONSE
BOOKING_NOT_FOUND
IDEMPOTENCY_KEY_REUSED
INVALID_IDEMPOTENCY_KEY
VALIDATION_FAILED
```

---

# 46. API Error Examples

Example:

```json
{
  "errorCode": "VEHICLE_UNAVAILABLE",
  "message": "Vehicle is not available for the requested period"
}
```

For invalid rental duration:

```json
{
  "errorCode": "MAX_RENTAL_DAYS_EXCEEDED",
  "message": "A vehicle cannot be booked for more than 21 days"
}
```

For idempotency conflict:

```json
{
  "errorCode": "IDEMPOTENCY_KEY_REUSED",
  "message": "Idempotency-Key was already used with a different request"
}
```

---

# 47. OpenAPI

The external credit-card payment contract is maintained at:

```text
src/main/resources/openapi/credit-card-payment-api.yaml
```

Maven generates the Java client automatically during:

```text
generate-sources
```

The OpenAPI Generator plugin version configured in the project is:

```text
7.25.0
```

Generated sources are under the Maven target/generated-sources area and should not be manually edited.

---

# 48. Code Quality

The project uses Spotless with Palantir Java Format.

Formatting/checking is integrated into the Maven lifecycle.

Run:

```bash
./mvnw spotless:check
```

or apply formatting:

```bash
./mvnw spotless:apply
```

---

# 49. Code Coverage

JaCoCo is configured in the Maven build.

Coverage reporting is generated during:

```text
verify
```

The resulting reports are available under:

```text
target/site/jacoco/
```

when the corresponding Maven lifecycle completes.

---

# 50. Docker Image

The project includes:

```text
Dockerfile
```

Build:

```bash
docker build -t car-booking-service .
```

Run:

```bash
docker run \
  -p 8080:8080 \
  -p 8081:8081 \
  car-booking-service
```

When running the application inside Docker, database and Kafka addresses must be configured according to the Docker network rather than assuming `localhost` refers to the host machine.

---

# 51. Local Development Architecture

The recommended local setup is:

```text
                 Local Machine
                       |
          +------------+------------+
          |            |            |
          v            v            v
      Spring Boot   PostgreSQL    Kafka
       :8080         :35432       :9092
          |
          +--------------------+
          |                    |
          v                    v
      WireMock               Zipkin
       :9090                 :9411
```

Management endpoints:

```text
:8081
```

---

# 52. Recommended Local Startup

Start infrastructure:

```bash
docker compose up -d
```

Verify:

```bash
docker compose ps
```

Then start the application:

```bash
./mvnw spring-boot:run
```

Verify health:

```text
http://localhost:8081/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

---

# 53. Typical Booking Flow Examples

## Cash

```text
POST /v1/bookings
paymentMethod=CASH

        |
        v

Validate
        |
        v
Calculate price
        |
        v
Confirm
        |
        v
201 Created
```

## Credit Card

```text
POST /v1/bookings
paymentMethod=CREDIT_CARD

        |
        v
Validate
        |
        v
Calculate price
        |
        v
Payment API
        |
        v
APPROVED
        |
        v
CONFIRMED
```

## Bank Transfer

```text
POST /v1/bookings
paymentMethod=BANK_TRANSFER

        |
        v

PENDING_PAYMENT
        |
        v
Kafka payment event
        |
        v
Consumer
        |
        v
Payment service
        |
        +------ partial ------> PENDING_PAYMENT
        |
        +------ full ---------> CONFIRMED
```

## Bank Transfer Timeout

```text
PENDING_PAYMENT
       |
       v
48-hour deadline reached
       |
       v
Cancellation job
       |
       v
CANCELLED
```

---

# 54. Production-Oriented Design Decisions

The implementation deliberately combines application-level and database-level protection.

## Idempotency

Application:

```text
request fingerprint
```

Database:

```text
unique idempotency key
```

## Vehicle overlap

Application:

```text
existsOverlappingBooking()
```

Database:

```text
PostgreSQL EXCLUDE constraint
```

## Bank payment duplicate protection

Application:

```text
duplicate-event handling
```

Database:

```text
UNIQUE(payment_id)
ON CONFLICT DO NOTHING
```

## Payment reliability

```text
Timeout
+
Retry
+
Exponential backoff
+
Metrics
+
Structured logs
```

## Kafka reliability

```text
Listener
+
Error Handler
+
Retry
+
DLT
```

---

# 55. Test Strategy

The project follows a layered testing approach.

```text
                   Testing Pyramid

                     Postman
                Acceptance / API
                       |
                       v
              Integration Tests
          Kafka / DB / WireMock
                       |
                       v
                  Unit Tests
          Business rules / services
```

## Unit tests

Fast tests for:

* pricing
* validation
* fingerprinting
* idempotency
* bank-transfer service logic

## Integration tests

Tests real infrastructure:

* PostgreSQL
* Kafka
* WireMock
* Spring application context

## Postman

Tests externally observable API behavior and operational flows.

---

# 56. Important Test Separation

`BookingApiIT` uses a mocked generated credit-card API for focused booking/API integration behavior.

`CreditCardPaymentServiceIT` exercises the real generated HTTP client against Docker WireMock.

This gives two different forms of coverage:

```text
BookingApiIT
    |
    +--> Booking/business logic
    |
    +--> mocked external payment client
```

and:

```text
CreditCardPaymentServiceIT
    |
    +--> real HTTP client
    |
    +--> Docker WireMock
    |
    +--> timeout/retry/HTTP behavior
```

This separation keeps tests focused while still providing real external-integration coverage.

---

# 57. Integration Test Reports

Failsafe reports are generated under:

```text
target/failsafe-reports/
```

Typical reports include:

```text
BookingApiIT
BookingCompleteWorkflowIT
CreditCardPaymentServiceIT
BankTransferPaymentEventConsumerIT
```

---

# 58. Troubleshooting

## PostgreSQL connection refused

Verify:

```bash
docker compose ps
```

and confirm PostgreSQL is exposed on:

```text
35432
```

Default application configuration:

```text
jdbc:postgresql://127.0.0.1:35432/velocity-motors-carrental
```

Override:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

---

## Kafka connection refused

Verify Kafka:

```bash
docker compose logs kafka
```

Default:

```text
localhost:9092
```

Override:

```text
KAFKA_BOOTSTRAP_SERVERS
```

---

## Credit-card payment failures

Verify WireMock:

```bash
docker compose ps credit-card-payment-mock
```

WireMock:

```text
http://localhost:9090
```

Admin API:

```text
http://localhost:9090/__admin
```

---

## Kafka event not processed

Check:

```bash
docker compose logs kafka
```

and application logs.

Verify:

```text
KAFKA_BOOTSTRAP_SERVERS
BANK_TRANSFER_PAYMENT_EVENTS_TOPIC
BANK_TRANSFER_PAYMENT_EVENTS_DLT_TOPIC
```

For test-support endpoints ensure:

```text
TEST_SUPPORT_ENABLED=true
```

---

## Booking unexpectedly unavailable

Check whether another active booking overlaps:

```text
vehicleId
rentalStart
rentalEnd
```

Cancelled bookings do not block the vehicle.

The PostgreSQL exclusion constraint also protects the database from overlapping active bookings.

---

# 59. Important Configuration Summary

| Configuration               | Default                            |
| --------------------------- | ---------------------------------- |
| Application port            | `8080`                             |
| Management port             | `8081`                             |
| PostgreSQL                  | `localhost:35432`                  |
| Kafka                       | `localhost:9092`                   |
| WireMock                    | `localhost:9090`                   |
| Zipkin                      | `localhost:9411`                   |
| Bank transfer topic         | `bank-transfer-payment-events`     |
| Bank transfer DLT           | `bank-transfer-payment-events.DLT` |
| Kafka listener concurrency  | `3`                                |
| Kafka retry attempts        | `3`                                |
| Kafka initial backoff       | `1s`                               |
| Credit-card connect timeout | `2s`                               |
| Credit-card read timeout    | `3s`                               |
| Credit-card retry attempts  | `3`                                |
| Cancellation fixed delay    | `60000 ms`                         |
| Cancellation batch size     | `500`                              |
| Maximum rental duration     | `21 days`                          |
| Idempotency-key maximum     | `100 characters`                   |
| Customer name maximum       | `50 characters`                    |
| Payment reference maximum   | `100 characters`                   |

---

# 60. API Quick Reference

| Method | Endpoint                                          | Purpose                          |
| ------ | ------------------------------------------------- | -------------------------------- |
| POST   | `/v1/bookings`                                    | Create booking                   |
| GET    | `/v1/bookings/{bookingId}`                        | Retrieve booking                 |
| POST   | `/test-support/v1/bank-transfer/payment-events`   | Publish test bank-transfer event |
| POST   | `/test-support/v1/bank-transfer/raw-events/{key}` | Publish raw Kafka event          |
| GET    | `/test-support/v1/bank-transfer/dlt-events`       | Search DLT                       |
| POST   | `/test-support/v1/cancellations/run`              | Run cancellation batch           |
| GET    | `/actuator/health`                                | Application health               |
| GET    | `/actuator/metrics`                               | Micrometer metrics               |
| GET    | `/actuator/prometheus`                            | Prometheus metrics               |

---

# 61. End-to-End Verification Checklist

Before considering a local deployment healthy:

### Infrastructure

* [ ] PostgreSQL is running
* [ ] Kafka is running
* [ ] WireMock is running
* [ ] Zipkin is running if tracing is required

### Application

* [ ] Application starts successfully
* [ ] Flyway migrations complete
* [ ] `/actuator/health` returns `UP`

### Booking

* [ ] CASH booking confirms
* [ ] DIGITAL_WALLET booking confirms
* [ ] CREDIT_CARD approved booking confirms
* [ ] BANK_TRANSFER starts as `PENDING_PAYMENT`

### Validation

* [ ] Invalid vehicle rejected
* [ ] Invalid dates rejected
* [ ] >21-day rental rejected
* [ ] Missing payment reference rejected
* [ ] Vehicle overlap rejected

### Idempotency

* [ ] Same key/same request replays booking
* [ ] Same key/different request rejected
* [ ] Long idempotency key rejected

### Credit Card

* [ ] Approved response works
* [ ] Rejected response works
* [ ] 404 handled
* [ ] 500 retry works
* [ ] Persistent failure exhausts retry
* [ ] Timeout handled

### Bank Transfer

* [ ] Partial payment remains pending
* [ ] Cumulative payment confirms
* [ ] Duplicate payment does not double-credit
* [ ] Invalid event reaches DLT
* [ ] Unknown booking reaches DLT

### Cancellation

* [ ] 48-hour deadline calculated
* [ ] Due booking is cancelled
* [ ] Cancelled booking no longer blocks vehicle

### Observability

* [ ] Structured JSON logs generated
* [ ] `X-Request-Id` available
* [ ] Trace/span information available
* [ ] Prometheus endpoint available
* [ ] Booking metrics available
* [ ] Credit-card metrics available
* [ ] Retry metrics available
* [ ] Bank-transfer metrics available

---

# 62. Useful Commands

### Start infrastructure

```bash
docker compose up -d
```

### Stop infrastructure

```bash
docker compose down
```

### Follow infrastructure logs

```bash
docker compose logs -f
```

### Build

```bash
./mvnw clean package
```

### Unit tests

```bash
./mvnw test
```

### Integration tests

```bash
./mvnw verify -DskipITs=false
```

### Start application

```bash
./mvnw spring-boot:run
```

### Format

```bash
./mvnw spotless:apply
```

### Check formatting

```bash
./mvnw spotless:check
```

### Build Docker image

```bash
docker build -t car-booking-service .
```

---

# 63. Summary

The Car Booking Service implements a complete booking workflow with:

```text
REST API
   |
   +--> Validation
   |
   +--> Pricing
   |
   +--> Vehicle availability
   |
   +--> Idempotency
   |
   +--> Payment processing
          |
          +--> Cash
          |
          +--> Digital Wallet
          |
          +--> Credit Card
          |       |
          |       +--> HTTP
          |       +--> Timeout
          |       +--> Retry
          |       +--> Metrics
          |
          +--> Bank Transfer
                  |
                  +--> Kafka
                  +--> Partial payment
                  +--> Cumulative payment
                  +--> Duplicate protection
                  +--> Retry
                  +--> DLT
                  +--> 48-hour cancellation

             |
             v
         PostgreSQL

             +
             
       Observability
             |
       +-----+------+
       |            |
    Metrics       Tracing
       |            |
   Prometheus     Zipkin
```

The project combines:

* Spring Boot
* Java 25
* PostgreSQL
* Flyway
* Kafka
* Resilience4j
* WireMock
* Testcontainers
* Micrometer
* Prometheus
* Zipkin
* structured JSON logging
* OpenAPI-generated clients
* unit testing
* integration testing
* Postman acceptance testing

The application therefore covers both the **business requirements** of the car-booking workflow and the supporting reliability/observability concerns required for a production-oriented implementation.
