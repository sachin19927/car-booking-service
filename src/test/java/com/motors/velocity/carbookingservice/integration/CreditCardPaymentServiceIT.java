package com.motors.velocity.carbookingservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motors.velocity.carbookingservice.integration.common.AbstractContainerIT;
import com.motors.velocity.carbookingservice.integration.common.BookingRequestFixtures;
import com.motors.velocity.carbookingservice.integration.common.WireMockExtensionHelper;
import com.motors.velocity.carbookingservice.repository.BankTransferPaymentEventRepository;
import com.motors.velocity.carbookingservice.repository.BookingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

class CreditCardPaymentServiceIT extends AbstractContainerIT {

    /** Read timeout is shortened for this class only, so the timeout scenario runs quickly. */
    @DynamicPropertySource
    static void shortReadTimeout(DynamicPropertyRegistry registry) {
        registry.add("credit-card-payment.timeout.read", () -> "800ms");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    BankTransferPaymentEventRepository bankTransferPaymentEventRepository;

    @BeforeEach
    void setUp() {
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
    void approvedPaymentConfirmsBookingViaRealHttpClient() throws Exception {
        String reference = "CC-APPROVED-1";
        WireMockExtensionHelper.mockApprovedPayment(reference);

        String request = bookingJson("Alice", "NL-678-AM", reference, "2032-01-01", "2032-01-02");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        assertThat(bookingRepository.findAll().getFirst().getBookingStatus().name())
                .isEqualTo("CONFIRMED");
        WireMockExtensionHelper.verifyPaymentStatusCall(reference);
    }

    @Test
    void rejectedPaymentReturnsBadRequestAndPersistsNoBooking() throws Exception {
        String reference = "CC-REJECTED-1";
        WireMockExtensionHelper.mockRejectedPayment(reference);

        String request = bookingJson("Bob", "NL-905-ZT", reference, "2032-02-01", "2032-02-02");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PAYMENT_NOT_APPROVED"));

        assertThat(bookingRepository.count()).isZero();
        WireMockExtensionHelper.verifyPaymentStatusCallCount(reference, 1);
    }

    @Test
    void paymentNotFoundReturns404() throws Exception {
        String reference = "CC-NOTFOUND-1";
        WireMockExtensionHelper.mockPaymentNotFound(reference);

        String request = bookingJson("Carol", "NL-214-RK", reference, "2032-03-01", "2032-03-02");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PAYMENT_NOT_FOUND"));

        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void transientServerErrorIsRetriedAndEventuallyConfirmsBooking() throws Exception {
        String reference = "CC-RETRY-1";
        WireMockExtensionHelper.mockTransientErrorThenApproved(reference);

        String request = bookingJson("Dave", "NL-431-HM", reference, "2032-04-01", "2032-04-02");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        assertThat(bookingRepository.findAll().getFirst().getBookingStatus().name())
                .isEqualTo("CONFIRMED");
        // 1 failing attempt + 1 successful retry
        WireMockExtensionHelper.verifyPaymentStatusCallCount(reference, 2);
    }

    @Test
    void persistentServerErrorExhaustsRetriesAndReturns500() throws Exception {
        String reference = "CC-ERROR-1";
        WireMockExtensionHelper.mockPaymentServiceError(reference);

        String request = bookingJson("Eve", "056-NL-JX", reference, "2032-05-01", "2032-05-02");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isInternalServerError());

        assertThat(bookingRepository.count()).isZero();
        // resilience4j max-attempts=3 in application-it.yml
        WireMockExtensionHelper.verifyPaymentStatusCallCount(reference, 3);
    }

    @Test
    void slowResponseExceedingReadTimeoutExhaustsRetriesAndReturns500() throws Exception {
        String reference = "CC-TIMEOUT-1";
        // 2s fixed delay against an 800ms read timeout guarantees a timeout on every attempt
        WireMockExtensionHelper.mockApprovedPaymentWithDelay(reference, 2000);

        String request = bookingJson("Frank", "721-NL-KR", reference, "2032-06-01", "2032-06-02");

        mockMvc.perform(post("/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isInternalServerError());

        assertThat(bookingRepository.count()).isZero();
        WireMockExtensionHelper.verifyPaymentStatusCallCount(reference, 3);
    }

    private String bookingJson(String customer, String vehicleId, String paymentReference, String start, String end) {
        return BookingRequestFixtures.bookingJson(
                objectMapper,
                customer,
                vehicleId,
                "LUXURY",
                "CREDIT_CARD",
                paymentReference,
                start + "T10:00:00Z",
                end + "T10:00:00Z");
    }
}
