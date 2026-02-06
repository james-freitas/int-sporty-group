package com.sportygroup.betsettler.rocketmq;

import com.sportygroup.betsettler.dto.BetSettlementDTO;

/**
 * Interface for bet settlement message producers.
 *
 * Allows for different implementations (real RocketMQ vs mock).
 */
public interface BetSettlementProducer {

    /**
     * Sends a bet settlement message.
     *
     * @param settlement The bet settlement to send
     */
    void sendSettlement(BetSettlementDTO settlement);
}