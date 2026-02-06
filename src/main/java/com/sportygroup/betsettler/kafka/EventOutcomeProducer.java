package com.sportygroup.betsettler.kafka;

import com.sportygroup.betsettler.config.KafkaTopicConfig;
import com.sportygroup.betsettler.dto.EventOutcomeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for publishing event outcome messages.
 *
 * Wrapper around KafkaTemplate that handles the sending of
 * EventOutcomeDTO messages to the configured Kafka topic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventOutcomeProducer {

    private final KafkaTemplate<String, EventOutcomeDTO> kafkaTemplate;
    private final KafkaTopicConfig topicConfig;

    /**
     * Sends an event outcome message to Kafka.
     *
     * Uses the eventId as the message key for proper partitioning,
     * ensuring all messages for the same event go to the same partition.
     *
     * @param eventOutcome The event outcome to send
     */
    public void sendEventOutcome(EventOutcomeDTO eventOutcome) {
        String topic = topicConfig.getEventOutcomes();
        String key = eventOutcome.getEventId();

        log.debug("Sending event outcome to Kafka - Topic: {}, Key: {}, Event: {}",
                topic, key, eventOutcome.getEventName());

        CompletableFuture<SendResult<String, EventOutcomeDTO>> future =
                kafkaTemplate.send(topic, key, eventOutcome);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event outcome sent successfully - Topic: {}, Partition: {}, Offset: {}, Event ID: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        eventOutcome.getEventId());
            } else {
                log.error("Failed to send event outcome - Event ID: {}, Error: {}",
                        eventOutcome.getEventId(), ex.getMessage(), ex);
            }
        });
    }
}
