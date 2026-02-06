package com.sportygroup.betsettler.repository;

import com.sportygroup.betsettler.entity.Bet;
import com.sportygroup.betsettler.entity.BetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository for Bet entity.
 *
 * Provides database access methods for bet operations.
 */
@Repository
public interface BetRepository extends JpaRepository<Bet, Long> {

    /**
     * Finds all bets for a specific event with a given status.
     *
     * This is the core query used for bet matching - it finds all pending
     * bets for an event that need to be settled when an outcome is published.
     *
     * @param eventId The event identifier
     * @param status The bet status (typically PENDING)
     * @return List of matching bets
     */
    List<Bet> findByEventIdAndStatus(String eventId, BetStatus status);

    /**
     * Finds all bets for a specific user.
     *
     * @param userId The user identifier
     * @return List of user's bets
     */
    List<Bet> findByUserId(String userId);

    /**
     * Finds all bets for a specific event.
     *
     * @param eventId The event identifier
     * @return List of bets for the event
     */
    List<Bet> findByEventId(String eventId);

    /**
     * Finds all bets with a specific status.
     *
     * @param status The bet status
     * @return List of bets with the given status
     */
    List<Bet> findByStatus(BetStatus status);
}