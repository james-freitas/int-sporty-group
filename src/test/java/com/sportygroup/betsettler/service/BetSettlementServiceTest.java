package com.sportygroup.betsettler.service;

import com.sportygroup.betsettler.dto.BetSettlementDTO;
import com.sportygroup.betsettler.entity.Bet;
import com.sportygroup.betsettler.entity.BetStatus;
import com.sportygroup.betsettler.repository.BetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BetSettlementService.
 */
@ExtendWith(MockitoExtension.class)
class BetSettlementServiceTest {

    @Mock
    private BetRepository betRepository;

    @InjectMocks
    private BetSettlementService betSettlementService;

    private Bet pendingBet;
    private BetSettlementDTO wonSettlement;
    private BetSettlementDTO lostSettlement;

    @BeforeEach
    void setUp() {
        pendingBet = Bet.builder()
                .betId(1L)
                .userId("USER-001")
                .eventId("EVT-001")
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId("TEAM-A")
                .betAmount(new BigDecimal("100.00"))
                .status(BetStatus.PENDING)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        wonSettlement = BetSettlementDTO.builder()
                .betId(1L)
                .userId("USER-001")
                .eventId("EVT-001")
                .eventWinnerId("TEAM-A")
                .predictedWinnerId("TEAM-A")
                .betAmount(new BigDecimal("100.00"))
                .won(true)
                .build();

        lostSettlement = BetSettlementDTO.builder()
                .betId(1L)
                .userId("USER-001")
                .eventId("EVT-001")
                .eventWinnerId("TEAM-B")
                .predictedWinnerId("TEAM-A")
                .betAmount(new BigDecimal("100.00"))
                .won(false)
                .build();
    }

    @Test
    void settleBet_WithWonBet_UpdatesStatusToWon() {
        // Given
        when(betRepository.findById(1L)).thenReturn(Optional.of(pendingBet));
        ArgumentCaptor<Bet> betCaptor = ArgumentCaptor.forClass(Bet.class);

        // When
        betSettlementService.settleBet(wonSettlement);

        // Then
        verify(betRepository).findById(1L);
        verify(betRepository).save(betCaptor.capture());

        Bet savedBet = betCaptor.getValue();
        assertThat(savedBet.getStatus(), equalTo(BetStatus.WON));
        assertThat(savedBet.getSettledAt(), is(notNullValue()));
        assertThat(savedBet.getSettledAt(), greaterThan(savedBet.getCreatedAt()));
    }

    @Test
    void settleBet_WithLostBet_UpdatesStatusToLost() {
        // Given
        when(betRepository.findById(1L)).thenReturn(Optional.of(pendingBet));
        ArgumentCaptor<Bet> betCaptor = ArgumentCaptor.forClass(Bet.class);

        // When
        betSettlementService.settleBet(lostSettlement);

        // Then
        verify(betRepository).findById(1L);
        verify(betRepository).save(betCaptor.capture());

        Bet savedBet = betCaptor.getValue();
        assertThat(savedBet.getStatus(), equalTo(BetStatus.LOST));
        assertThat(savedBet.getSettledAt(), is(notNullValue()));
    }

    @Test
    void settleBet_WithNonExistentBet_ThrowsException() {
        // Given
        when(betRepository.findById(999L)).thenReturn(Optional.empty());

        BetSettlementDTO settlement = BetSettlementDTO.builder()
                .betId(999L)
                .won(true)
                .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> betSettlementService.settleBet(settlement)
        );

        assertThat(exception.getMessage(), containsString("Bet not found"));
        verify(betRepository).findById(999L);
        verify(betRepository, never()).save(any(Bet.class));
    }

    @Test
    void getBetById_WithExistingBet_ReturnsBet() {
        // Given
        when(betRepository.findById(1L)).thenReturn(Optional.of(pendingBet));

        // When
        Bet result = betSettlementService.getBetById(1L);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result.getBetId(), equalTo(1L));
        assertThat(result.getUserId(), equalTo("USER-001"));
        verify(betRepository).findById(1L);
    }

    @Test
    void getBetById_WithNonExistentBet_ThrowsException() {
        // Given
        when(betRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> betSettlementService.getBetById(999L)
        );

        assertThat(exception.getMessage(), containsString("Bet not found"));
    }
}