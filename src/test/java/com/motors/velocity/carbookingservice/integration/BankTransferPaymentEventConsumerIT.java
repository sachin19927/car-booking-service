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
import com.motors.velocity.carbookingservice.model.BookingStatus;
import com.motors.velocity.carbookingservice.repository.BankTransferPaymentEventRepository;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public class BankTransferPaymentEventConsumerIT extends AbstractContainerIT {

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

    @Value("${app.kafka.bank-transfer-payment-events.dlt-topic}")
    String dltTopic;

    KafkaTestHelper kafkaTestHelper;

    @BeforeEach
    void setup() {
        kafkaTestHelper = new KafkaTestHelper(getKafkaBootstrapServers());
        bankTransferPaymentEventRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        bankTransferPaymentEventRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void validPaymentEventConfirmsBookingAsynchronously() throws Exception {
        UUID bookingId = createBankTransferBooking("Alice", "VH-NL-347", "TXN400000001", "2033-01-01", "2033-01-02");

        publishEvent("PAY-400000001", "TXN400000001 " + bookingId, new BigDecimal("50.00"));
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    CarBooking booking = bookingRepository.findById(bookingId).orElseThrow();
                    assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
                    assertThat(booking.getPaymentReceivedAmount()).isEqualByComparingTo("50.00");
                });
    }

    @Test
    void partialPaymentEventKeepBookingPendingAsynchronously() throws Exception {
        UUID bookingId = createBankTransferBooking("Bob", "VH-NL-347", "TXN400000002", "2033-02-01", "2033-02-03");

        publishEvent("PAY-400000002", "TXN400000002 " + bookingId, new BigDecimal("30.00"));
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    CarBooking booking = bookingRepository.findById(bookingId).orElseThrow();
                    assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
                    assertThat(booking.getPaymentReceivedAmount()).isEqualByComparingTo("30.00");
                });
    }

    @Test
    void duplicatePaymentEventIsIgnoredIdempotently() throws Exception {
        UUID bookingId = createBankTransferBooking("Carol", "VH-NL-594", "TXN400000003", "2033-03-01", "2033-03-02");

        publishEvent("PAY-400000003", "TXN400000003 " + bookingId, new BigDecimal("25.00"));
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    CarBooking booking = bookingRepository.findById(bookingId).orElseThrow();
                    assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
                    assertThat(booking.getPaymentReceivedAmount()).isEqualByComparingTo("25.00");
                });

        publishEvent("PAY-400000003", "TXN400000003 " + bookingId, new BigDecimal("25.00"));
        Thread.sleep(3000);

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getPaymentReceivedAmount())
                .isEqualByComparingTo("25.00");
        assertThat(bankTransferPaymentEventRepository.count()).isEqualTo(1);
    }

    @Test
    void malformedJsonPayloadIsRoutedToDeadLetterTopic() throws Exception {
        String marker = "malformed-marker-" + UUID.randomUUID();
        kafkaTestHelper.produceRawMessage(topic, "malformed-key", "{ not-valid-json " + marker);

        ConsumerRecord<String, String> dltRecord = kafkaTestHelper.awaitRecord(
                dltTopic,
                "dlt-verify-malformed-" + UUID.randomUUID(),
                Duration.ofSeconds(20),
                value -> value != null && value.contains(marker));

        assertThat(dltRecord)
                .as("malformed event should be published to the DLT")
                .isNotNull();
    }

    @Test
    void unknownBookingReferenceEventIsRoutedToDeadLetterTopic() throws Exception {
        String unknownPaymentId = "PAY-UNKNOWN-" + UUID.randomUUID();
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                unknownPaymentId,
                "NL00BANK",
                new BigDecimal("10.00"),
                "TXN999999999 " + UUID.randomUUID()); // well-formed but no matching booking exists

        kafkaTestHelper.produceRawMessage(topic, unknownPaymentId, objectMapper.writeValueAsString(event));

        ConsumerRecord<String, String> dltRecord = kafkaTestHelper.awaitRecord(
                dltTopic,
                "dlt-verify-unknown-" + UUID.randomUUID(),
                Duration.ofSeconds(20),
                value -> value != null && value.contains(unknownPaymentId));

        assertThat(dltRecord)
                .as("event referencing an unknown booking should be published to the DLT")
                .isNotNull();
    }

    private void publishEvent(String paymentId, String transactionDetails, BigDecimal amount) throws Exception {
        BankTransferPaymentEvent event =
                new BankTransferPaymentEvent(paymentId, "NL00BANK", amount, transactionDetails);
        kafkaTestHelper.produceRawMessage(topic, paymentId, objectMapper.writeValueAsString(event));
    }

    private UUID createBankTransferBooking(
            String customer, String vehicleId, String paymentReference, String start, String end) throws Exception {
        String request = BookingRequestFixtures.bookingJson(
                objectMapper,
                customer,
                vehicleId,
                "COMPACT",
                "BANK_TRANSFER",
                paymentReference,
                start + "T10:00:00Z",
                end + "T10:00:00Z");
        String body = mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("bookingId").asText());
    }
}
