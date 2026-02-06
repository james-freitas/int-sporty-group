package com.sportygroup.betsettler.rocketmq;

import com.sportygroup.betsettler.dto.BetSettlementDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of BetSettlementProducer.
 *
 * Instead of sending messages to RocketMQ, this implementation
 * simply logs the settlement information. Useful for testing
 * and development without RocketMQ infrastructure.
 *
 * Activated when application.rocketmq.enabled=false
 */
@Component
@ConditionalOnProperty(name = "application.rocketmq.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MockBetSettlementProducer implements BetSettlementProducer {

    private final ObjectMapper objectMapper;
    private final MockBetSettlementConsumer mockConsumer;

    /**
     * Logs the bet settlement instead of sending to RocketMQ.
     * Also processes the settlement directly via mock consumer.
     *
     * @param settlement The bet settlement to log and process
     */
    @Override
    public void sendSettlement(BetSettlementDTO settlement) {
        log.info("========================================");
        log.info("MOCK ROCKETMQ - Bet Settlement Message");
        log.info("========================================");
        log.info("Bet ID: {}", settlement.getBetId());
        log.info("User ID: {}", settlement.getUserId());
        log.info("Event ID: {}", settlement.getEventId());
        log.info("Event Market: {}", settlement.getEventMarketId());
        log.info("Predicted Winner: {}", settlement.getPredictedWinnerId());
        log.info("Actual Winner: {}", settlement.getEventWinnerId());
        log.info("Bet Amount: ${}", settlement.getBetAmount());
        log.info("Result: {}", settlement.getWon() ? "WON" : "LOST");

        try {
            String json = objectMapper.writeValueAsString(settlement);
            log.info("JSON Payload: {}", json);
        } catch (Exception e) {
            log.error("Error serializing settlement to JSON", e);
        }

        log.info("========================================");

        // Process settlement directly via mock consumer
        mockConsumer.processSettlement(settlement);
    }
}