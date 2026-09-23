package com.motors.velocity.carbookingservice.config;

import com.motors.velocity.carbookingservice.exception.InvalidBankTransferPaymentEventException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    NewTopic bankTransferPaymentEventsTopic() {
        KafkaProperties.BankTransferPaymentEvents bankTransferPaymentEvents =
                kafkaProperties.bankTransferPaymentEvents();
        return new NewTopic(
                bankTransferPaymentEvents.topic(),
                bankTransferPaymentEvents.partitions(),
                bankTransferPaymentEvents.replicationFactor());
    }

    @Bean
    NewTopic bankTransferPaymentEventsDltTopic() {
        KafkaProperties.BankTransferPaymentEvents bankTransferPaymentEvents =
                kafkaProperties.bankTransferPaymentEvents();
        return new NewTopic(
                bankTransferPaymentEvents.dltTopic(),
                bankTransferPaymentEvents.partitions(),
                bankTransferPaymentEvents.replicationFactor());
    }

    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {

        KafkaProperties.Retry retry = kafkaProperties.consumer().retry();
        String dltTopic = kafkaProperties.bankTransferPaymentEvents().dltTopic();

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, exception) -> new TopicPartition(dltTopic, record.partition()));

        ExponentialBackOffWithMaxRetries backOff =
                new ExponentialBackOffWithMaxRetries(Math.max(0, retry.maxAttempts() - 1));
        backOff.setInitialInterval(retry.initialBackoff().toMillis());
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(InvalidBankTransferPaymentEventException.class);
        return errorHandler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> bankTransferKafkaListenerContainerFactory(
            org.springframework.kafka.core.ConsumerFactory<String, String> consumerFactory,
            CommonErrorHandler kafkaErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(kafkaProperties.listener().concurrency());
        return factory;
    }
}
