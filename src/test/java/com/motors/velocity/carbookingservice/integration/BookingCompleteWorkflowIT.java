package com.motors.velocity.carbookingservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motors.velocity.carbookingservice.dto.BankTransferPaymentEvent;
import com.motors.velocity.carbookingservice.entity.CarBooking;
import com.motors.velocity.carbookingservice.integration.common.AbstractContainerIT;
import com.motors.velocity.carbookingservice.integration.common.BookingRequestFixtures;
import com.motors.velocity.carbookingservice.integration.common.KafkaTestHelper;
import com.motors.velocity.carbookingservice.integration.common.WireMockExtensionHelper;
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.repository.BankTransferPaymentEventRepository;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class BookingCompleteWorkflowIT extends AbstractContainerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    BankTransferPaymentEventRepository bankTransferPaymentEventRepository;

    @Value("${app.kafka.bank-transfer-payment-events.topic}")
    String topic;

    KafkaTestHelper kafkaTestHelper;

    @BeforeEach
    void setUp() {
        kafkaTestHelper = new KafkaTestHelper(getKafkaBootstrapServers());
        WireMockExtensionHelper.resetAll();
        bankTransferPaymentEventRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        bankTransferPaymentEventRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void digitalWalletBookingIsConfirmedImmediately() throws Exception {
        String request = BookingRequestFixtures.bookingJson(
                objectMapper,
                "Workflow Wallet",
                "NL-431-HM",
                "SUV",
                "DIGITAL_WALLET",
                null,
                "2034-01-01T10:00:00Z",
                "2034-01-02T10:00:00Z");

        String body = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID bookingId =
                UUID.fromString(objectMapper.readTree(body).get("bookingId").asText());
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void creditCardBookingIsConfirmedThroughRealWireMockBackedClient() throws Exception {
        String reference = "WF-CC-APPROVED-1";
        WireMockExtensionHelper.mockApprovedPayment(reference);

        String request = BookingRequestFixtures.bookingJson(
                objectMapper,
                "Workflow Credit Card",
                "NL-678-AM",
                "LUXURY",
                "CREDIT_CARD",
                reference,
                "2034-02-01T10:00:00Z",
                "2034-02-02T10:00:00Z");

        String body = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID bookingId =
                UUID.fromString(objectMapper.readTree(body).get("bookingId").asText());
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
        WireMockExtensionHelper.verifyPaymentStatusCall(reference);
    }

    @Test
    void bankTransferBookingIsConfirmedAsynchronouslyThroughRealKafkaConsumer() throws Exception {
        String paymentReference = "TXN500000001";
        String request = BookingRequestFixtures.bookingJson(
                objectMapper,
                "Workflow Bank Transfer",
                "NL-214-RK",
                "COMPACT",
                "BANK_TRANSFER",
                paymentReference,
                "2034-03-01T10:00:00Z",
                "2034-03-02T10:00:00Z");

        String body = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID bookingId =
                UUID.fromString(objectMapper.readTree(body).get("bookingId").asText());

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.PENDING_PAYMENT);

        // Full payment, produced by a real Kafka producer and consumed by the real @KafkaListener
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY-500000001", "NL00BANK", new BigDecimal("50.00"), paymentReference + " " + bookingId);
        kafkaTestHelper.produceRawMessage(topic, event.paymentId(), objectMapper.writeValueAsString(event));

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    CarBooking booking = bookingRepository.findById(bookingId).orElseThrow();
                    assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
                    assertThat(booking.getPaymentReceivedAmount()).isEqualByComparingTo("50.00");
                });
    }

    @Test
    void duplicateBankTransferKafkaEventDoesNotDoubleCreditThePayment() throws Exception {
        String paymentReference = "TXN500000002";
        String request = BookingRequestFixtures.bookingJson(
                objectMapper,
                "Workflow Duplicate",
                "NL-905-ZT",
                "COMPACT",
                "BANK_TRANSFER",
                paymentReference,
                "2034-04-01T10:00:00Z",
                "2034-04-03T10:00:00Z");

        String body = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID bookingId =
                UUID.fromString(objectMapper.readTree(body).get("bookingId").asText());

        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY-500000002", "NL00BANK", new BigDecimal("20.00"), paymentReference + " " + bookingId);
        String payload = objectMapper.writeValueAsString(event);

        kafkaTestHelper.produceRawMessage(topic, event.paymentId(), payload);
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> assertThat(bookingRepository
                                .findById(bookingId)
                                .orElseThrow()
                                .getPaymentReceivedAmount())
                        .isEqualByComparingTo("20.00"));

        // Re-publish the identical event (same paymentId) - the consumer must ignore it
        kafkaTestHelper.produceRawMessage(topic, event.paymentId(), payload);
        Thread.sleep(3000);

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getPaymentReceivedAmount())
                .isEqualByComparingTo("20.00");
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.PENDING_PAYMENT);
    }
}
