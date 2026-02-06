package com.sportygroup.betsettler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Data Transfer Object for Bet Settlement messages.
 *
 * Used for RocketMQ messaging when a bet needs to be settled.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BetSettlementDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier of the bet to be settled
     */
    @JsonProperty("betId")
    private Long betId;

    /**
     * Identifier of the user who placed the bet
     */
    @JsonProperty("userId")
    private String userId;

    /**
     * Identifier of the event
     */
    @JsonProperty("eventId")
    private String eventId;

    /**
     * The actual winner of the event
     */
    @JsonProperty("eventWinnerId")
    private String eventWinnerId;

    /**
     * The user's predicted winner
     */
    @JsonProperty("predictedWinnerId")
    private String predictedWinnerId;

    /**
     * Amount wagered on this bet
     */
    @JsonProperty("betAmount")
    private BigDecimal betAmount;

    /**
     * Whether the bet was won (true) or lost (false)
     */
    @JsonProperty("won")
    private Boolean won;

    /**
     * Name of the event market
     */
    @JsonProperty("eventMarketId")
    private String eventMarketId;
}
