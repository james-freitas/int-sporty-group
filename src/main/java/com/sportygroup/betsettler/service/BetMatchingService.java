package com.sportygroup.betsettler.service;

import com.sportygroup.betsettler.dto.BetSettlementDTO;
import com.sportygroup.betsettler.dto.EventOutcomeDTO;
import com.sportygroup.betsettler.entity.Bet;
import com.sportygroup.betsettler.entity.BetStatus;
import com.sportygroup.betsettler.repository.BetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for matching event outcomes with pending bets.
 *
 * This service contains the core business logic for determining which
 * bets need to be settled when an event outcome is published.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BetMatchingService {

    private final BetRepository betRepository;

    /**
     * Matches pending bets with an event outcome.
     *
     * Finds all pending bets for the given event and creates settlement
     * DTOs for each bet, determining whether the bet was won or lost
     * by comparing the predicted winner with the actual winner.
     *
     * @param eventOutcome The event outcome to match against
     * @return List of bet settlement DTOs ready for processing
     */
    public List<BetSettlementDTO> matchBets(EventOutcomeDTO eventOutcome) {
        log.info("Matching bets for event: {} with winner: {}",
                eventOutcome.getEventId(), eventOutcome.getEventWinnerId());

        // Find all pending bets for this event
        List<Bet> pendingBets = betRepository.findByEventIdAndStatus(
                eventOutcome.getEventId(),
                BetStatus.PENDING
        );

        log.info("Found {} pending bets for event: {}",
                pendingBets.size(), eventOutcome.getEventId());

        if (pendingBets.isEmpty()) {
            log.warn("No pending bets found for event: {}", eventOutcome.getEventId());
            return List.of();
        }

        // Convert bets to settlement DTOs
        List<BetSettlementDTO> settlements = pendingBets.stream()
                .map(bet -> createSettlement(bet, eventOutcome))
                .collect(Collectors.toList());

        long wonBets = settlements.stream().filter(BetSettlementDTO::getWon).count();
        long lostBets = settlements.size() - wonBets;

        log.info("Created {} settlements for event {} - Won: {}, Lost: {}",
                settlements.size(), eventOutcome.getEventId(), wonBets, lostBets);

        return settlements;
    }

    /**
     * Creates a bet settlement DTO from a bet and event outcome.
     *
     * Determines if the bet was won by comparing the predicted winner
     * with the actual winner from the event outcome.
     *
     * @param bet The bet to settle
     * @param eventOutcome The event outcome
     * @return BetSettlementDTO ready for processing
     */
    private BetSettlementDTO createSettlement(Bet bet, EventOutcomeDTO eventOutcome) {
        // Determine if the bet was won
        boolean won = bet.getEventWinnerId().equals(eventOutcome.getEventWinnerId());

        log.debug("Bet {} for user {} - Predicted: {}, Actual: {}, Result: {}",
                bet.getBetId(), bet.getUserId(),
                bet.getEventWinnerId(), eventOutcome.getEventWinnerId(),
                won ? "WON" : "LOST");

        return BetSettlementDTO.builder()
                .betId(bet.getBetId())
                .userId(bet.getUserId())
                .eventId(bet.getEventId())
                .eventMarketId(bet.getEventMarketId())
                .eventWinnerId(eventOutcome.getEventWinnerId())
                .predictedWinnerId(bet.getEventWinnerId())
                .betAmount(bet.getBetAmount())
                .won(won)
                .build();
    }

    /**
     * Gets statistics about pending bets for an event.
     *
     * @param eventId The event identifier
     * @return Count of pending bets
     */
    public long getPendingBetCount(String eventId) {
        return betRepository.findByEventIdAndStatus(eventId, BetStatus.PENDING).size();
    }
}
