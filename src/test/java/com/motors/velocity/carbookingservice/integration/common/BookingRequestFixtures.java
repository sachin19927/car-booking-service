package com.motors.velocity.carbookingservice.integration.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZonedDateTime;

public final class BookingRequestFixtures {

    private BookingRequestFixtures() {}

    public static String bookingJson(
            ObjectMapper objectMapper,
            String customer,
            String vehicleId,
            String category,
            String paymentMethod,
            String paymentReference,
            String startDate,
            String endDate) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("customerName", customer);
        node.put("vehicleId", vehicleId);
        node.put("vehicleCategory", category);
        node.put("paymentMethod", paymentMethod);
        node.put("startDate", ZonedDateTime.parse(startDate).toString());
        node.put("endDate", ZonedDateTime.parse(endDate).toString());
        if (paymentReference != null) {
            node.put("paymentReference", paymentReference);
        }
        return node.toString();
    }
}
