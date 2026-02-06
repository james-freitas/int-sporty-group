package com.sportygroup.betsettler.service;

import com.sportygroup.betsettler.dto.BetSettlementDTO;
import com.sportygroup.betsettler.dto.EventOutcomeDTO;
import com.sportygroup.betsettler.entity.Bet;
import com.sportygroup.betsettler.entity.BetStatus;
import com.sportygroup.betsettler.repository.BetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BetMatchingService.
 */
@ExtendWith(MockitoExtension.class)
class BetMatchingServiceTest {

    @Mock
    private BetRepository betRepository;

    @InjectMocks
    private BetMatchingService betMatchingService;

    private EventOutcomeDTO eventOutcome;
    private List<Bet> pendingBets;

    @BeforeEach
    void setUp() {
        eventOutcome = EventOutcomeDTO.builder()
                .eventId("EVT-001")
                .eventName("Test Match")
                .eventWinnerId("TEAM-A")
                .build();

        // Create test bets - some will win, some will lose
        Bet winningBet1 = Bet.builder()
                .betId(1L)
                .userId("USER-001")
                .eventId("EVT-001")
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId("TEAM-A") // Matches outcome
                .betAmount(new BigDecimal("100.00"))
                .status(BetStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Bet winningBet2 = Bet.builder()
                .betId(2L)
                .userId("USER-002")
                .eventId("EVT-001")
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId("TEAM-A") // Matches outcome
                .betAmount(new BigDecimal("50.00"))
                .status(BetStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Bet losingBet = Bet.builder()
                .betId(3L)
                .userId("USER-003")
                .eventId("EVT-001")
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId("TEAM-B") // Doesn't match outcome
                .betAmount(new BigDecimal("75.00"))
                .status(BetStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        pendingBets = Arrays.asList(winningBet1, winningBet2, losingBet);
    }

    @Test
    void matchBets_WithPendingBets_ReturnsCorrectSettlements() {
        // Given
        when(betRepository.findByEventIdAndStatus(
                eq("EVT-001"),
                eq(BetStatus.PENDING)
        )).thenReturn(pendingBets);

        // When
        List<BetSettlementDTO> settlements = betMatchingService.matchBets(eventOutcome);

        // Then
        assertThat(settlements, hasSize(3));
        verify(betRepository).findByEventIdAndStatus("EVT-001", BetStatus.PENDING);

        // Verify winning bets
        List<BetSettlementDTO> wonSettlements = settlements.stream()
                .filter(BetSettlementDTO::getWon)
                .toList();
        assertThat(wonSettlements, hasSize(2));
        assertThat(wonSettlements, everyItem(hasProperty("eventWinnerId", equalTo("TEAM-A"))));

        // Verify losing bets
        List<BetSettlementDTO> lostSettlements = settlements.stream()
                .filter(s -> !s.getWon())
                .toList();
        assertThat(lostSettlements, hasSize(1));
        assertThat(lostSettlements.get(0).getBetId(), equalTo(3L));
    }

    @Test
    void matchBets_WithNoPendingBets_ReturnsEmptyList() {
        // Given
        when(betRepository.findByEventIdAndStatus(
                any(String.class),
                eq(BetStatus.PENDING)
        )).thenReturn(List.of());

        // When
        List<BetSettlementDTO> settlements = betMatchingService.matchBets(eventOutcome);

        // Then
        assertThat(settlements, is(empty()));
        verify(betRepository).findByEventIdAndStatus("EVT-001", BetStatus.PENDING);
    }

    @Test
    void matchBets_CorrectlyMapsSettlementData() {
        // Given
        when(betRepository.findByEventIdAndStatus(
                eq("EVT-001"),
                eq(BetStatus.PENDING)
        )).thenReturn(pendingBets);

        // When
        List<BetSettlementDTO> settlements = betMatchingService.matchBets(eventOutcome);

        // Then
        BetSettlementDTO firstSettlement = settlements.get(0);
        assertThat(firstSettlement.getBetId(), equalTo(1L));
        assertThat(firstSettlement.getUserId(), equalTo("USER-001"));
        assertThat(firstSettlement.getEventId(), equalTo("EVT-001"));
        assertThat(firstSettlement.getEventMarketId(), equalTo("MATCH_WINNER"));
        assertThat(firstSettlement.getEventWinnerId(), equalTo("TEAM-A"));
        assertThat(firstSettlement.getPredictedWinnerId(), equalTo("TEAM-A"));
        assertThat(firstSettlement.getBetAmount(), equalTo(new BigDecimal("100.00")));
        assertThat(firstSettlement.getWon(), is(true));
    }

    @Test
    void getPendingBetCount_ReturnsCorrectCount() {
        // Given
        when(betRepository.findByEventIdAndStatus(
                eq("EVT-001"),
                eq(BetStatus.PENDING)
        )).thenReturn(pendingBets);

        // When
        long count = betMatchingService.getPendingBetCount("EVT-001");

        // Then
        assertThat(count, equalTo(3L));
        verify(betRepository).findByEventIdAndStatus("EVT-001", BetStatus.PENDING);
    }
}