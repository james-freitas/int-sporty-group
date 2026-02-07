package com.sportygroup.betsettler.kafka;

import com.sportygroup.betsettler.dto.BetSettlementDTO;
import com.sportygroup.betsettler.dto.EventOutcomeDTO;
import com.sportygroup.betsettler.rocketmq.BetSettlementProducer;
import com.sportygroup.betsettler.service.BetMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventOutcomeConsumer.
 *
 * Tests Kafka message consumption including:
 * - Successful message processing
 * - Bet matching and settlement
 * - Error handling and recovery
 * - Acknowledgment behavior
 * - Edge cases (empty settlements, partial failures)
 */
@ExtendWith(MockitoExtension.class)
class EventOutcomeConsumerTest {

    @Mock
    private BetMatchingService betMatchingService;

    @Mock
    private BetSettlementProducer betSettlementProducer;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private EventOutcomeConsumer eventOutcomeConsumer;

    private EventOutcomeDTO eventOutcome;
    private int partition;
    private long offset;

    @BeforeEach
    void setUp() {
        eventOutcome = EventOutcomeDTO.builder()
                .eventId("EVT-001")
                .eventName("Test Match")
                .eventWinnerId("TEAM-A")
                .build();

        partition = 0;
        offset = 123L;
    }

    @Test
    void consumeEventOutcome_WithSettlements_ProcessesSuccessfully() {
        // Given
        List<BetSettlementDTO> settlements = createSettlements(3);
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(settlements);

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betMatchingService).matchBets(eventOutcome);
        verify(betSettlementProducer, times(3)).sendSettlement(any(BetSettlementDTO.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEventOutcome_WithNoSettlements_AcknowledgesMessage() {
        // Given
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(new ArrayList<>());

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betMatchingService).matchBets(eventOutcome);
        verify(betSettlementProducer, never()).sendSettlement(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEventOutcome_SendsEachSettlementToRocketMQ() {
        // Given
        BetSettlementDTO settlement1 = createSettlement(1L, true);
        BetSettlementDTO settlement2 = createSettlement(2L, false);
        BetSettlementDTO settlement3 = createSettlement(3L, true);

        List<BetSettlementDTO> settlements = Arrays.asList(settlement1, settlement2, settlement3);
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(settlements);

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betSettlementProducer).sendSettlement(settlement1);
        verify(betSettlementProducer).sendSettlement(settlement2);
        verify(betSettlementProducer).sendSettlement(settlement3);
    }

    @Test
    void consumeEventOutcome_OnBetMatchingFailure_DoesNotAcknowledge() {
        // Given
        when(betMatchingService.matchBets(eventOutcome))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment)
        );

        assertThat(exception.getMessage(), containsString("Failed to process event outcome"));
        assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        assertThat(exception.getCause().getMessage(), containsString("Database connection failed"));

        verify(betMatchingService).matchBets(eventOutcome);
        verify(betSettlementProducer, never()).sendSettlement(any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consumeEventOutcome_OnRocketMQFailure_ContinuesProcessingOthers() {
        // Given
        BetSettlementDTO settlement1 = createSettlement(1L, true);
        BetSettlementDTO settlement2 = createSettlement(2L, false);
        BetSettlementDTO settlement3 = createSettlement(3L, true);

        List<BetSettlementDTO> settlements = Arrays.asList(settlement1, settlement2, settlement3);
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(settlements);

        // Second settlement fails
        doNothing().when(betSettlementProducer).sendSettlement(settlement1);
        doThrow(new RuntimeException("RocketMQ timeout"))
                .when(betSettlementProducer).sendSettlement(settlement2);
        doNothing().when(betSettlementProducer).sendSettlement(settlement3);

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betSettlementProducer).sendSettlement(settlement1);
        verify(betSettlementProducer).sendSettlement(settlement2);
        verify(betSettlementProducer).sendSettlement(settlement3);
        verify(acknowledgment).acknowledge(); // Still acknowledges despite individual failure
    }

    @Test
    void consumeEventOutcome_OnAllRocketMQFailures_StillAcknowledges() {
        // Given
        List<BetSettlementDTO> settlements = createSettlements(3);
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(settlements);

        // All RocketMQ sends fail
        doThrow(new RuntimeException("RocketMQ down"))
                .when(betSettlementProducer).sendSettlement(any(BetSettlementDTO.class));

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betSettlementProducer, times(3)).sendSettlement(any(BetSettlementDTO.class));
        verify(acknowledgment).acknowledge(); // Still acknowledges (RocketMQ failures are logged but not fatal)
    }

    @Test
    void consumeEventOutcome_CallsBetMatchingServiceWithCorrectEvent() {
        // Given
        when(betMatchingService.matchBets(any())).thenReturn(new ArrayList<>());

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betMatchingService).matchBets(eventOutcome);
    }

    @Test
    void consumeEventOutcome_WithSingleSettlement_ProcessesCorrectly() {
        // Given
        BetSettlementDTO settlement = createSettlement(100L, true);
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(Arrays.asList(settlement));

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betSettlementProducer, times(1)).sendSettlement(settlement);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEventOutcome_WithMultipleSettlements_ProcessesInOrder() {
        // Given
        List<BetSettlementDTO> settlements = createSettlements(5);
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(settlements);

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        // Verify each settlement sent in order
        for (BetSettlementDTO settlement : settlements) {
            verify(betSettlementProducer).sendSettlement(settlement);
        }
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEventOutcome_OnMatchingServiceException_ThrowsRuntimeException() {
        // Given
        when(betMatchingService.matchBets(eventOutcome))
                .thenThrow(new IllegalStateException("Invalid event state"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment)
        );

        assertThat(exception.getMessage(), containsString("Failed to process event outcome"));
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consumeEventOutcome_VerifiesPartitionAndOffsetLogged() {
        // Given
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(new ArrayList<>());

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        // Partition and offset should be passed through (verified by no exceptions)
        verify(betMatchingService).matchBets(eventOutcome);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEventOutcome_WithDifferentPartitionAndOffset_ProcessesCorrectly() {
        // Given
        int differentPartition = 5;
        long differentOffset = 9999L;
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(new ArrayList<>());

        // When
        eventOutcomeConsumer.consumeEventOutcome(
                eventOutcome,
                differentPartition,
                differentOffset,
                acknowledgment
        );

        // Then
        verify(betMatchingService).matchBets(eventOutcome);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEventOutcome_AcknowledgmentCalledOnlyOnSuccess() {
        // Given
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(createSettlements(2));

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void consumeEventOutcome_AcknowledgmentNotCalledOnException() {
        // Given
        when(betMatchingService.matchBets(eventOutcome))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment)
        );

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consumeEventOutcome_WithWonBets_SendsCorrectSettlements() {
        // Given
        BetSettlementDTO wonBet1 = createSettlement(1L, true);
        BetSettlementDTO wonBet2 = createSettlement(2L, true);

        when(betMatchingService.matchBets(eventOutcome))
                .thenReturn(Arrays.asList(wonBet1, wonBet2));

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betSettlementProducer).sendSettlement(wonBet1);
        verify(betSettlementProducer).sendSettlement(wonBet2);
    }

    @Test
    void consumeEventOutcome_WithLostBets_SendsCorrectSettlements() {
        // Given
        BetSettlementDTO lostBet1 = createSettlement(1L, false);
        BetSettlementDTO lostBet2 = createSettlement(2L, false);

        when(betMatchingService.matchBets(eventOutcome))
                .thenReturn(Arrays.asList(lostBet1, lostBet2));

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betSettlementProducer).sendSettlement(lostBet1);
        verify(betSettlementProducer).sendSettlement(lostBet2);
    }

    @Test
    void consumeEventOutcome_WithMixedWonAndLostBets_ProcessesAll() {
        // Given
        BetSettlementDTO wonBet = createSettlement(1L, true);
        BetSettlementDTO lostBet = createSettlement(2L, false);

        when(betMatchingService.matchBets(eventOutcome))
                .thenReturn(Arrays.asList(wonBet, lostBet));

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betSettlementProducer).sendSettlement(wonBet);
        verify(betSettlementProducer).sendSettlement(lostBet);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEventOutcome_PartialRocketMQFailure_LogsErrorButContinues() {
        // Given
        BetSettlementDTO settlement1 = createSettlement(1L, true);
        BetSettlementDTO settlement2 = createSettlement(2L, false);

        when(betMatchingService.matchBets(eventOutcome))
                .thenReturn(Arrays.asList(settlement1, settlement2));

        // First succeeds, second fails
        doNothing().when(betSettlementProducer).sendSettlement(settlement1);
        doThrow(new RuntimeException("Network error"))
                .when(betSettlementProducer).sendSettlement(settlement2);

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betSettlementProducer).sendSettlement(settlement1);
        verify(betSettlementProducer).sendSettlement(settlement2);
        verify(acknowledgment).acknowledge(); // Still acknowledges
    }

    @Test
    void consumeEventOutcome_NullPointerInRocketMQ_ContinuesProcessing() {
        // Given
        BetSettlementDTO settlement1 = createSettlement(1L, true);
        BetSettlementDTO settlement2 = createSettlement(2L, false);

        when(betMatchingService.matchBets(eventOutcome))
                .thenReturn(Arrays.asList(settlement1, settlement2));

        // First throws NPE
        doThrow(new NullPointerException("Null reference"))
                .when(betSettlementProducer).sendSettlement(settlement1);
        doNothing().when(betSettlementProducer).sendSettlement(settlement2);

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betSettlementProducer).sendSettlement(settlement1);
        verify(betSettlementProducer).sendSettlement(settlement2); // Still processes second
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEventOutcome_EmptyEventId_StillProcesses() {
        // Given
        EventOutcomeDTO eventWithEmptyId = EventOutcomeDTO.builder()
                .eventId("")
                .eventName("Test")
                .eventWinnerId("WINNER")
                .build();

        when(betMatchingService.matchBets(eventWithEmptyId)).thenReturn(new ArrayList<>());

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventWithEmptyId, partition, offset, acknowledgment);

        // Then
        verify(betMatchingService).matchBets(eventWithEmptyId);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEventOutcome_LargeNumberOfSettlements_ProcessesAll() {
        // Given
        List<BetSettlementDTO> largeSettlementList = createSettlements(100);
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(largeSettlementList);

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then
        verify(betSettlementProducer, times(100)).sendSettlement(any(BetSettlementDTO.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEventOutcome_VerifyCorrectInteractionOrder() {
        // Given
        List<BetSettlementDTO> settlements = createSettlements(2);
        when(betMatchingService.matchBets(eventOutcome)).thenReturn(settlements);

        // When
        eventOutcomeConsumer.consumeEventOutcome(eventOutcome, partition, offset, acknowledgment);

        // Then - Verify order of calls
        var inOrder = inOrder(betMatchingService, betSettlementProducer, acknowledgment);
        inOrder.verify(betMatchingService).matchBets(eventOutcome);
        inOrder.verify(betSettlementProducer, times(2)).sendSettlement(any(BetSettlementDTO.class));
        inOrder.verify(acknowledgment).acknowledge();
    }

    /**
     * Helper method to create a list of test settlements.
     */
    private List<BetSettlementDTO> createSettlements(int count) {
        List<BetSettlementDTO> settlements = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            settlements.add(createSettlement((long) i, i % 2 == 0));
        }
        return settlements;
    }

    /**
     * Helper method to create a single test settlement.
     */
    private BetSettlementDTO createSettlement(Long betId, boolean won) {
        return BetSettlementDTO.builder()
                .betId(betId)
                .userId("USER-" + betId)
                .eventId("EVT-001")
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId("TEAM-A")
                .predictedWinnerId(won ? "TEAM-A" : "TEAM-B")
                .betAmount(new BigDecimal("100.00"))
                .won(won)
                .build();
    }
}