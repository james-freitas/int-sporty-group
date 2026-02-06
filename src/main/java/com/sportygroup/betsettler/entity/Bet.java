package com.sportygroup.betsettler.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a bet placed by a user.
 *
 * A bet is associated with a specific sports event and contains
 * the user's prediction of the outcome.
 */
@Entity
@Table(name = "bet", indexes = {
        @Index(name = "idx_event_id_status", columnList = "eventId,status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bet {

    /**
     * Unique identifier for the bet (auto-generated)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long betId;

    /**
     * Identifier of the user who placed the bet
     */
    @Column(nullable = false, length = 50)
    private String userId;

    /**
     * Identifier of the sports event this bet is for
     */
    @Column(nullable = false, length = 50)
    private String eventId;

    /**
     * Type of market (e.g., MATCH_WINNER, OVER_UNDER, etc.)
     */
    @Column(nullable = false, length = 50)
    private String eventMarketId;

    /**
     * The predicted winner or outcome
     */
    @Column(nullable = false, length = 50)
    private String eventWinnerId;

    /**
     * Amount wagered on this bet
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal betAmount;

    /**
     * Current status of the bet
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BetStatus status = BetStatus.PENDING;

    /**
     * Timestamp when the bet was created
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the bet was settled (nullable for pending bets)
     */
    @Column
    private LocalDateTime settledAt;

    /**
     * Pre-persist callback to set creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Marks this bet as won and sets the settlement timestamp.
     */
    public void markAsWon() {
        this.status = BetStatus.WON;
        this.settledAt = LocalDateTime.now();
    }

    /**
     * Marks this bet as lost and sets the settlement timestamp.
     */
    public void markAsLost() {
        this.status = BetStatus.LOST;
        this.settledAt = LocalDateTime.now();
    }
}
