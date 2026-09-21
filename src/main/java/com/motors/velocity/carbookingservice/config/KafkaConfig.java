package com.motors.velocity.carbookingservice.config;

import com.motors.velocity.carbookingservice.exception.InvalidBankTransferPaymentEventException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic bankTransferPaymentEventsTopic(
            @Value("${app.kafka.bank-transfer-payment-events.partitions:3}") int partitions,
            @Value("${app.kafka.bank-transfer-payment-events.replication-factor:1}") short replicationFactor,
            @Value("${app.kafka.bank-transfer-payment-events.topic:bank-transfer-payment-events}") String topic) {
        return new NewTopic(topic, partitions, replicationFactor);
    }

    @Bean
    NewTopic bankTransferPaymentEventsDltTopic(
            @Value("${app.kafka.bank-transfer-payment-events.partitions:3}") int partitions,
            @Value("${app.kafka.bank-transfer-payment-events.replication-factor:1}") short replicationFactor,
            @Value("${app.kafka.bank-transfer-payment-events.dlt-topic:bank-transfer-payment-events.DLT}")
                    String topic) {
        return new NewTopic(topic, partitions, replicationFactor);
    }

    @Bean
    CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.consumer.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.kafka.consumer.retry.initial-backoff:1000ms}") java.time.Duration initialBackoff,
            @Value("${app.kafka.bank-transfer-payment-events.dlt-topic:bank-transfer-payment-events.DLT}")
                    String dltTopic) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, exception) -> new TopicPartition(dltTopic, record.partition()));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(Math.max(0, maxAttempts - 1));
        backOff.setInitialInterval(initialBackoff.toMillis());
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(InvalidBankTransferPaymentEventException.class);
        return errorHandler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> bankTransferKafkaListenerContainerFactory(
            org.springframework.kafka.core.ConsumerFactory<String, String> consumerFactory,
            CommonErrorHandler kafkaErrorHandler,
            @Value("${app.kafka.listener.concurrency:3}") int concurrency) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(concurrency);
        return factory;
    }
}
