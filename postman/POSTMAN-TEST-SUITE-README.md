# Car Booking Service — Comprehensive Postman Test Suite

Derived from the uploaded source tree, the existing Postman collection, and the existing JUnit/unit/integration tests.

## Important corrections

- Idempotency tests explicitly send `Idempotency-Key`.
- Test-support payment-event endpoints assert **202 Accepted**, matching the current controller.
- Cancellation asserts `cancelledCount`, matching the current controller.
- Cancellation uses dynamic `now + 24h` rental start, so the 48h payment deadline is already due without using invalid past rental dates.
- Credit-card scenarios configure the Docker WireMock Admin API so APPROVED/REJECTED/400/404/500/retry/invalid-response/timeout cases can actually be exercised.
- Unique IDs/markers are used where repeated runs could otherwise collide.

## Coverage

| Area | Cases |
|---|---|
| Positive | CASH; DIGITAL_WALLET; CREDIT_CARD approved |
| Credit card | approved; rejected; 400 invalid reference; 404 not found; persistent 500; transient 500→approved; invalid response; timeout/retry exhaustion |
| Bank transfer | pending; partial payment; cumulative full payment; duplicate event |
| Kafka/DLT | malformed JSON; unknown booking; invalid event field |
| Idempotency | replay; different request; key >100 chars |
| Validation | invalid vehicle; invalid vehicle format; missing/oversized customer; missing dates/category/payment; missing references; long reference; past start; bad/equal dates; >21 days; exactly 21 days; malformed JSON |
| Availability | overlap rejected; adjacent booking allowed |
| Cancellation | overdue booking; run batch; verify cancelled; cancelled booking no longer blocks vehicle |
| Retrieval | existing booking; not found |
| Observability | health; booking metrics; credit-card metrics; retry metrics; bank-transfer metrics |

## Prerequisites

- Application: `http://localhost:8080`
- Management: `http://localhost:8081`
- Docker WireMock: `http://localhost:9090`
- Kafka and PostgreSQL running
- `app.test-support.enabled=true` for test-support cases
- Test topic/DLT configured when running against the integration environment

## Postman vs JUnit responsibility

Postman validates externally observable API/business behavior. The existing JUnit suite remains the authoritative place for internal persistence assertions such as exact `totalAmount`, `paymentReceivedAmount`, payment deadline, duplicate-event database records, concurrent booking race behavior, and exact Micrometer counter increments.
