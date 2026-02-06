package com.sportygroup.betsettler.kafka;

import com.sportygroup.betsettler.dto.BetSettlementDTO;
import com.sportygroup.betsettler.dto.EventOutcomeDTO;
import com.sportygroup.betsettler.rocketmq.BetSettlementProducer;
import com.sportygroup.betsettler.service.BetMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka consumer for event outcome messages.
 *
 * Listens to the event-outcomes topic and triggers bet matching
 * and settlement processing for each event outcome received.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventOutcomeConsumer {

    private final BetMatchingService betMatchingService;
    private final BetSettlementProducer betSettlementProducer;

    /**
     * Consumes event outcome messages from Kafka.
     *
     * For each event outcome:
     * 1. Finds pending bets matching the event
     * 2. Determines which bets were won/lost
     * 3. Sends settlement messages to RocketMQ
     *
     * Uses manual acknowledgment to ensure message processing reliability.
     *
     * @param eventOutcome The event outcome message
     * @param partition The Kafka partition
     * @param offset The message offset
     * @param acknowledgment Manual acknowledgment handle
     */
    @KafkaListener(
            topics = "${application.kafka.topics.event-outcomes}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEventOutcome(
            @Payload EventOutcomeDTO eventOutcome,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received event outcome from Kafka - Event ID: {}, Winner: {}, Partition: {}, Offset: {}",
                eventOutcome.getEventId(), eventOutcome.getEventWinnerId(), partition, offset);

        try {
            // Match pending bets with the event outcome
            List<BetSettlementDTO> settlements = betMatchingService.matchBets(eventOutcome);

            if (settlements.isEmpty()) {
                log.info("No bets to settle for event: {}", eventOutcome.getEventId());
            } else {
                log.info("Processing {} bet settlements for event: {}",
                        settlements.size(), eventOutcome.getEventId());

                // Send each settlement to RocketMQ
                for (BetSettlementDTO settlement : settlements) {
                    try {
                        betSettlementProducer.sendSettlement(settlement);
                        log.debug("Sent settlement to RocketMQ - Bet ID: {}, Won: {}",
                                settlement.getBetId(), settlement.getWon());
                    } catch (Exception e) {
                        log.error("Failed to send settlement to RocketMQ - Bet ID: {}, Error: {}",
                                settlement.getBetId(), e.getMessage(), e);
                        // Continue processing other settlements even if one fails
                    }
                }

                log.info("Successfully processed all settlements for event: {}",
                        eventOutcome.getEventId());
            }

            // Acknowledge successful processing
            acknowledgment.acknowledge();
            log.debug("Acknowledged Kafka message - Partition: {}, Offset: {}", partition, offset);

        } catch (Exception e) {
            log.error("Error processing event outcome - Event ID: {}, Error: {}",
                    eventOutcome.getEventId(), e.getMessage(), e);
            // Don't acknowledge - message will be reprocessed
            throw new RuntimeException("Failed to process event outcome", e);
        }
    }
}
