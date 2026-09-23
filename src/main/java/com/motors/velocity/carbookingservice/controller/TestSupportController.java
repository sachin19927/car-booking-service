package com.motors.velocity.carbookingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motors.velocity.carbookingservice.config.KafkaProperties;
import com.motors.velocity.carbookingservice.dto.BankTransferPaymentEvent;
import com.motors.velocity.carbookingservice.service.BookingCancellationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test-support/v1")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.test-support", name = "enabled", havingValue = "true")
public class TestSupportController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaProperties kafkaProperties;
    private final BookingCancellationService bookingCancellationService;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @PostMapping("/bank-transfer/payment-events")
    public ResponseEntity<Map<String, String>> publishPaymentEvent(@RequestBody BankTransferPaymentEvent event)
            throws Exception {
        String topic = kafkaProperties.bankTransferPaymentEvents().topic();
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(topic, event.paymentId(), payload).get();
        return ResponseEntity.accepted().body(Map.of("status", "published", "topic", topic, "key", event.paymentId()));
    }

    @PostMapping("/bank-transfer/raw-events/{key}")
    public ResponseEntity<Map<String, String>> publishRawEvent(@PathVariable String key, @RequestBody String rawBody)
            throws Exception {
        String topic = kafkaProperties.bankTransferPaymentEvents().topic();
        kafkaTemplate.send(topic, key, rawBody).get();
        return ResponseEntity.accepted().body(Map.of("status", "published", "topic", topic, "key", key));
    }

    @GetMapping("/bank-transfer/dlt-events")
    public ResponseEntity<Map<String, Object>> peekDeadLetterTopic(
            @RequestParam String containing, @RequestParam(defaultValue = "10") long timeoutSeconds) {
        String dltTopic = kafkaProperties.bankTransferPaymentEvents().dltTopic();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-support-dlt-peek-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(dltTopic));
            long deadline = System.currentTimeMillis()
                    + Duration.ofSeconds(timeoutSeconds).toMillis();
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.value() != null && record.value().contains(containing)) {
                        return ResponseEntity.ok(
                                Map.of("found", true, "topic", dltTopic, "key", record.key(), "value", record.value()));
                    }
                }
            }
        }
        return ResponseEntity.status(404).body(Map.of("found", false, "topic", dltTopic));
    }

    @PostMapping("/cancellations/run")
    public ResponseEntity<Map<String, Object>> runCancellationBatch() {
        int cancelled = bookingCancellationService.cancelDueBookings(Instant.now());
        return ResponseEntity.ok(Map.of("cancelledCount", cancelled));
    }
}
