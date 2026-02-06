package com.sportygroup.betsettler.service;

import com.sportygroup.betsettler.dto.BetSettlementDTO;
import com.sportygroup.betsettler.entity.Bet;
import com.sportygroup.betsettler.repository.BetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for settling bets in the database.
 *
 * This service handles the final step of bet settlement - updating
 * the bet status in the database based on settlement messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BetSettlementService {

    private final BetRepository betRepository;

    /**
     * Settles a bet based on the settlement DTO.
     *
     * Updates the bet status to WON or LOST and sets the settlement timestamp.
     * This operation is transactional to ensure data consistency.
     *
     * @param settlement The bet settlement information
     * @throws IllegalArgumentException if bet is not found
     */
    @Transactional
    public void settleBet(BetSettlementDTO settlement) {
        log.info("Settling bet - Bet ID: {}, User: {}, Event: {}, Won: {}",
                settlement.getBetId(), settlement.getUserId(),
                settlement.getEventId(), settlement.getWon());

        // Fetch the bet from database
        Bet bet = betRepository.findById(settlement.getBetId())
                .orElseThrow(() -> {
                    log.error("Bet not found - Bet ID: {}", settlement.getBetId());
                    return new IllegalArgumentException("Bet not found: " + settlement.getBetId());
                });

        // Update bet status based on outcome
        if (settlement.getWon()) {
            bet.markAsWon();
            log.info("Bet marked as WON - Bet ID: {}, User: {}, Amount: {}",
                    bet.getBetId(), bet.getUserId(), bet.getBetAmount());
        } else {
            bet.markAsLost();
            log.info("Bet marked as LOST - Bet ID: {}, User: {}, Amount: {}",
                    bet.getBetId(), bet.getUserId(), bet.getBetAmount());
        }

        // Save updated bet
        betRepository.save(bet);

        log.info("Successfully settled bet - Bet ID: {}, Status: {}, Settled at: {}",
                bet.getBetId(), bet.getStatus(), bet.getSettledAt());
    }

    /**
     * Retrieves a bet by its ID.
     *
     * @param betId The bet identifier
     * @return The bet entity
     * @throws IllegalArgumentException if bet is not found
     */
    public Bet getBetById(Long betId) {
        return betRepository.findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found: " + betId));
    }
}