package com.sportygroup.betsettler.entity;

/**
 * Enum representing the status of a bet.
 */
public enum BetStatus {
    /**
     * Bet is pending settlement - waiting for event outcome
     */
    PENDING,

    /**
     * Bet has been settled and the user won
     */
    WON,

    /**
     * Bet has been settled and the user lost
     */
    LOST
}
