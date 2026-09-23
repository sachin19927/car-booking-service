package com.motors.velocity.carbookingservice.integration.common;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public class WireMockExtensionHelper {

    static final WireMockServer WIRE_MOCK_SERVER = new WireMockServer(
            WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true));

    public static int getWireMockPort() {
        return WIRE_MOCK_SERVER.port();
    }

    public static String getWireMockBaseUrl() {
        return "http://localhost:" + getWireMockPort();
    }

    public static WireMockServer getWireMockExtension() {
        return WIRE_MOCK_SERVER;
    }

    public static void mockApprovedPayment(String paymentReference) {
        configureWireMock();
        String responseBody =
                "{" + "  \"status\": \"APPROVED\"," + "  \"lastUpdateDate\": \"2026-09-22T09:48:31Z\"" + "}";

        client().stubFor(WireMock.post(WireMock.urlPathEqualTo("/credit-card-payment-api/payment-status"))
                .withHeader("Content-Type", WireMock.matching("application/json"))
                .withRequestBody(WireMock.containing(paymentReference))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                        .withFixedDelay(100)));
    }

    public static void mockRejectedPayment(String paymentReference) {
        configureWireMock();
        String responseBody =
                "{" + "  \"status\": \"REJECTED\"," + "  \"lastUpdateDate\": \"2026-09-22T09:48:31Z\"" + "}";

        client().stubFor(WireMock.post(WireMock.urlPathEqualTo("/credit-card-payment-api/payment-status"))
                .withHeader("Content-Type", WireMock.matching("application/json"))
                .withRequestBody(WireMock.containing(paymentReference))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                        .withFixedDelay(100)));
    }

    public static void mockPaymentNotFound(String paymentReference) {
        configureWireMock();
        String responseBody = "{" + "  \"error\": \"Payment not found\"" + "}";

        client().stubFor(WireMock.post(WireMock.urlPathEqualTo("/credit-card-payment-api/payment-status"))
                .withHeader("Content-Type", WireMock.matching("application/json"))
                .withRequestBody(WireMock.containing(paymentReference))
                .willReturn(WireMock.aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                        .withFixedDelay(100)));
    }

    public static void mockPaymentServiceError(String paymentReference) {
        configureWireMock();
        String responseBody = "{" + "  \"error\": \"Internal server error\"" + "}";

        client().stubFor(WireMock.post(WireMock.urlPathEqualTo("/credit-card-payment-api/payment-status"))
                .withHeader("Content-Type", WireMock.matching("application/json"))
                .withRequestBody(WireMock.containing(paymentReference))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                        .withFixedDelay(100)));
    }

    public static void mockTransientErrorThenApproved(String paymentReference) {
        configureWireMock();
        String scenario = "retry-then-approved-" + paymentReference;
        String errorBody = "{ \"error\": \"Internal server error\" }";
        String approvedBody =
                "{" + "  \"status\": \"APPROVED\"," + "  \"lastUpdateDate\": \"2026-09-22T09:48:31Z\"" + "}";

        client().stubFor(WireMock.post(WireMock.urlPathEqualTo("/credit-card-payment-api/payment-status"))
                .withRequestBody(WireMock.containing(paymentReference))
                .inScenario(scenario)
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willSetStateTo("RETRY_SUCCEEDED")
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorBody)));

        client().stubFor(WireMock.post(WireMock.urlPathEqualTo("/credit-card-payment-api/payment-status"))
                .withRequestBody(WireMock.containing(paymentReference))
                .inScenario(scenario)
                .whenScenarioStateIs("RETRY_SUCCEEDED")
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(approvedBody)));
    }

    public static void mockApprovedPaymentWithDelay(String paymentReference, int delayMs) {
        configureWireMock();
        String responseBody =
                "{" + "  \"status\": \"APPROVED\"," + "  \"lastUpdateDate\": \"2026-09-22T09:48:31Z\"" + "}";

        client().stubFor(WireMock.post(WireMock.urlPathEqualTo("/credit-card-payment-api/payment-status"))
                .withHeader("Content-Type", WireMock.matching("application/json"))
                .withRequestBody(WireMock.containing(paymentReference))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                        .withFixedDelay(delayMs)));
    }

    public static void resetAll() {
        configureWireMock();
        client().reset();
    }

    public static void verifyPaymentStatusCall(String paymentReference) {
        configureWireMock();
        client().verify(WireMock.postRequestedFor(WireMock.urlPathEqualTo("/credit-card-payment-api/payment-status"))
                .withRequestBody(WireMock.containing(paymentReference)));
    }

    private static void configureWireMock() {
        WireMock.configureFor("localhost", getWireMockPort());
    }

    private static WireMock client() {
        return new WireMock("localhost", getWireMockPort());
    }

    public static void verifyPaymentStatusCallCount(String paymentReference, int count) {
        configureWireMock();
        client().verify(
                        count,
                        WireMock.postRequestedFor(WireMock.urlPathEqualTo("/credit-card-payment-api/payment-status"))
                                .withRequestBody(WireMock.containing(paymentReference)));
    }
}
