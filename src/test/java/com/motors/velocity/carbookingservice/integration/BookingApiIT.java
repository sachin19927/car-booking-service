package com.motors.velocity.carbookingservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motors.velocity.carbookingservice.client.payment.api.DefaultApi;
import com.motors.velocity.carbookingservice.client.payment.model.PaymentStatusResponse;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.repository.BankTransferPaymentEventRepository;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import com.motors.velocity.carbookingservice.service.BankTransferPaymentService;
import com.motors.velocity.carbookingservice.service.BookingCancellationService;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingApiIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    BankTransferPaymentService bankTransferPaymentService;

    @Autowired
    private BankTransferPaymentEventRepository bankTransferPaymentEventRepository;

    @Autowired
    BookingCancellationService cancellationService;

    @Autowired
    ObjectMapper objectMapper;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    DefaultApi creditCardPaymentApi;

    @BeforeEach
    void setUp() {
        bankTransferPaymentEventRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        bankTransferPaymentEventRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void cashBookingIsConfirmedAndAmountIsPersisted() throws Exception {
        String request = bookingJson(
                "Alice", "VH-NL-347", "COMPACT", "CASH", null, "2030-01-01T10:00:00Z", "2030-01-02T10:00:00Z");

        String body = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "cash-1")
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUIDAssertions.assertValid(objectMapper.readTree(body).get("bookingId").asText());
        CarBooking booking = bookingRepository.findAll().getFirst();
        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void bankTransferStartsPendingWith48HourDeadlineAndFullPaymentConfirms() throws Exception {
        String request = bookingJson(
                "Bob",
                "VH-NL-821",
                "SEDAN",
                "BANK_TRANSFER",
                "TXN123456789",
                "2030-02-01T10:00:00Z",
                "2030-02-03T10:00:00Z");

        String body = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "bank-1")
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID bookingId = java.util.UUID.fromString(
                objectMapper.readTree(body).get("bookingId").asText());
        CarBooking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("140.00");
        assertThat(booking.getPaymentDeadline())
                .isEqualTo(booking.getRentalStart().minusSeconds(48 * 3600L));

        bankTransferPaymentService.processPaymentEvent(
                new com.motors.velocity.carbookingservice.dto.BankTransferPaymentEvent(
                        "PAY-1", "NL00BANK", new BigDecimal("50.00"), "TXN123456789 " + bookingId));
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.PENDING_PAYMENT);

        bankTransferPaymentService.processPaymentEvent(
                new com.motors.velocity.carbookingservice.dto.BankTransferPaymentEvent(
                        "PAY-2", "NL00BANK", new BigDecimal("90.00"), "TXN123456789 " + bookingId));
        CarBooking confirmed = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(confirmed.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmed.getPaymentReceivedAmount()).isEqualByComparingTo("140.00");
    }

    @Test
    void duplicateBankPaymentEventDoesNotIncreaseReceivedAmount() throws Exception {
        String request = bookingJson(
                "Carol",
                "VH-NL-594",
                "COMPACT",
                "BANK_TRANSFER",
                "TXN223456789",
                "2030-03-01T10:00:00Z",
                "2030-03-02T10:00:00Z");
        String body = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID bookingId = java.util.UUID.fromString(
                objectMapper.readTree(body).get("bookingId").asText());

        var event = new com.motors.velocity.carbookingservice.dto.BankTransferPaymentEvent(
                "PAY-DUP", "NL00BANK", new BigDecimal("25.00"), "TXN223456789 " + bookingId);
        bankTransferPaymentService.processPaymentEvent(event);
        bankTransferPaymentService.processPaymentEvent(event);

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getPaymentReceivedAmount())
                .isEqualByComparingTo("25.00");
    }

    @Test
    void overdueBankTransferIsAutomaticallyCancelled() throws Exception {
        ZonedDateTime rentalStart = ZonedDateTime.now(java.time.ZoneOffset.UTC).plusHours(24);

        ZonedDateTime rentalEnd = rentalStart.plusHours(24);

        String request = bookingJson(
                "Dave",
                "056-NL-JX",
                "COMPACT",
                "BANK_TRANSFER",
                "TXN323456789",
                rentalStart.toString(),
                rentalEnd.toString());

        String body = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID bookingId =
                UUID.fromString(objectMapper.readTree(body).get("bookingId").asText());

        int cancelled = cancellationService.cancelDueBookings(java.time.Instant.now());

        assertThat(cancelled).isEqualTo(1);

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void sameIdempotencyKeyReplaysExistingBooking() throws Exception {
        String request = bookingJson(
                "Eve", "721-NL-KR", "SUV", "DIGITAL_WALLET", null, "2030-04-01T10:00:00Z", "2030-04-02T10:00:00Z");

        String first = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "replay-1")
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "replay-1")
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(second).get("bookingId").asText())
                .isEqualTo(objectMapper.readTree(first).get("bookingId").asText());
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void reusedIdempotencyKeyWithDifferentRequestIsRejected() throws Exception {
        String first = bookingJson(
                "Frank", "438-NL-PT", "COMPACT", "CASH", null, "2030-05-01T10:00:00Z", "2030-05-02T10:00:00Z");
        String different = bookingJson(
                "Different", "438-NL-PT", "COMPACT", "CASH", null, "2030-05-01T10:00:00Z", "2030-05-02T10:00:00Z");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "reuse-1")
                        .content(first))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "reuse-1")
                        .content(different))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creditCardApprovedIsConfirmed() throws Exception {
        when(creditCardPaymentApi.paymentStatusPost(any()))
                .thenReturn(new PaymentStatusResponse().status(PaymentStatusResponse.StatusEnum.APPROVED));

        String request = bookingJson(
                "Grace",
                "NL-678-AM",
                "LUXURY",
                "CREDIT_CARD",
                "CC-123",
                "2030-06-01T10:00:00Z",
                "2030-06-02T10:00:00Z");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        assertThat(bookingRepository.findAll().getFirst().getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void creditCardRejectedIsNotConfirmed() throws Exception {
        when(creditCardPaymentApi.paymentStatusPost(any()))
                .thenReturn(new PaymentStatusResponse().status(PaymentStatusResponse.StatusEnum.REJECTED));

        String request = bookingJson(
                "Grace Rejected",
                "NL-678-AM",
                "LUXURY",
                "CREDIT_CARD",
                "CC-REJECTED",
                "2030-06-10T10:00:00Z",
                "2030-06-11T10:00:00Z");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void invalidRentalPeriodIsRejected() throws Exception {
        String request = bookingJson(
                "Henry", "NL-214-RK", "COMPACT", "CASH", null, "2030-07-02T10:00:00Z", "2030-07-01T10:00:00Z");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rentalLongerThan21DaysIsRejected() throws Exception {
        String request = bookingJson(
                "Ivy", "NL-905-ZT", "COMPACT", "CASH", null, "2030-08-01T10:00:00Z", "2030-08-23T10:00:01Z");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentBookingsForSameVehicleAllowOnlyOneWinner() throws Exception {
        String first = bookingJson(
                "Concurrent One", "NL-431-HM", "COMPACT", "CASH", null, "2031-01-01T10:00:00Z", "2031-01-05T10:00:00Z");
        String second = bookingJson(
                "Concurrent Two", "NL-431-HM", "COMPACT", "CASH", null, "2031-01-01T10:00:00Z", "2031-01-05T10:00:00Z");

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var start = new java.util.concurrent.CountDownLatch(1);
            var firstResult = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/v1/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Idempotency-Key", "concurrent-1")
                                .content(first))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            var secondResult = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/v1/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Idempotency-Key", "concurrent-2")
                                .content(second))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            start.countDown();
            int firstStatus = firstResult.get();
            int secondStatus = secondResult.get();

            assertThat(java.util.List.of(firstStatus, secondStatus)).contains(201);
            assertThat(bookingRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void overlappingVehicleBookingIsRejected() throws Exception {
        String first = bookingJson(
                "Jack", "NL-431-HM", "COMPACT", "CASH", null, "2030-09-01T10:00:00Z", "2030-09-05T10:00:00Z");
        String second = bookingJson(
                "Kate", "NL-431-HM", "COMPACT", "CASH", null, "2030-09-03T10:00:00Z", "2030-09-06T10:00:00Z");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(second))
                .andExpect(status().isBadRequest());
    }

    private String bookingJson(
            String customer,
            String vehicle,
            String category,
            String payment,
            String reference,
            String start,
            String end)
            throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("customerName", customer);
        node.put("vehicleId", vehicle);
        node.put("startDate", ZonedDateTime.parse(start).toString());
        node.put("endDate", ZonedDateTime.parse(end).toString());
        node.put("vehicleCategory", category);
        node.put("paymentMethod", payment);
        if (reference != null) node.put("paymentReference", reference);
        return objectMapper.writeValueAsString(node);
    }

    private static final class UUIDAssertions {
        static void assertValid(String value) {
            java.util.UUID.fromString(value);
        }
    }
}
