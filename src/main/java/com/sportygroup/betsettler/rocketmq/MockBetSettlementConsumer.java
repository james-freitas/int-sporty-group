package com.sportygroup.betsettler.rocketmq;

import com.sportygroup.betsettler.dto.BetSettlementDTO;
import com.sportygroup.betsettler.service.BetSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mock consumer that processes settlements directly when RocketMQ is disabled.
 *
 * Since we're using a mock producer that only logs, we need to actually
 * settle the bets. This component listens to a custom event and processes
 * settlements directly, bypassing RocketMQ entirely.
 *
 * Activated when application.rocketmq.enabled=false
 */
@Component
@ConditionalOnProperty(name = "application.rocketmq.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MockBetSettlementConsumer {

    private final BetSettlementService betSettlementService;

    /**
     * Processes bet settlement directly (mock RocketMQ behavior).
     *
     * In mock mode, settlements are processed synchronously instead
     * of being sent through RocketMQ.
     *
     * @param settlement The bet settlement to process
     */
    public void processSettlement(BetSettlementDTO settlement) {
        log.info("MOCK CONSUMER - Processing bet settlement directly - Bet ID: {}",
                settlement.getBetId());

        try {
            betSettlementService.settleBet(settlement);
            log.info("MOCK CONSUMER - Successfully settled bet - Bet ID: {}, Status: {}",
                    settlement.getBetId(), settlement.getWon() ? "WON" : "LOST");
        } catch (Exception e) {
            log.error("MOCK CONSUMER - Failed to settle bet - Bet ID: {}, Error: {}",
                    settlement.getBetId(), e.getMessage(), e);
        }
    }
}