package com.sportygroup.betsettler.service;

import com.sportygroup.betsettler.config.KafkaTopicConfig;
import com.sportygroup.betsettler.dto.EventOutcomeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing event outcomes to Kafka.
 *
 * This service acts as the entry point for event outcome publication,
 * sending messages to the Kafka topic for downstream processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventOutcomeService {

    private final KafkaTemplate<String, EventOutcomeDTO> kafkaTemplate;
    private final KafkaTopicConfig topicConfig;

    /**
     * Publishes an event outcome to Kafka.
     *
     * The event outcome is sent to the configured Kafka topic with the
     * eventId as the message key for proper partitioning.
     *
     * @param eventOutcome The event outcome to publish
     */
    public void publishEventOutcome(EventOutcomeDTO eventOutcome) {
        log.info("Publishing event outcome to Kafka - Event ID: {}, Winner: {}",
                eventOutcome.getEventId(), eventOutcome.getEventWinnerId());

        String topic = topicConfig.getEventOutcomes();
        String key = eventOutcome.getEventId();

        CompletableFuture<SendResult<String, EventOutcomeDTO>> future =
                kafkaTemplate.send(topic, key, eventOutcome);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully sent event outcome to Kafka - Topic: {}, Key: {}, Partition: {}, Offset: {}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send event outcome to Kafka - Event ID: {}, Error: {}",
                        eventOutcome.getEventId(), ex.getMessage(), ex);
                throw new RuntimeException("Failed to publish event outcome", ex);
            }
        });
    }
}
