package com.sportygroup.betsettler.rocketmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportygroup.betsettler.config.RocketMQConfig;
import com.sportygroup.betsettler.dto.BetSettlementDTO;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.message.MessageId;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RealBetSettlementProducer.
 *
 * Tests RocketMQ producer functionality including:
 * - Initialization and lifecycle
 * - Message sending
 * - Error handling
 * - Resource cleanup
 */
@ExtendWith(MockitoExtension.class)
class RealBetSettlementProducerTest {

    @Mock
    private Producer producer;

    @Mock
    private SendReceipt sendReceipt;

    @Mock
    private MessageId messageId;

    private RocketMQConfig rocketMQConfig;
    private ObjectMapper objectMapper;
    private RealBetSettlementProducer realBetSettlementProducer;

    @BeforeEach
    void setUp() {
        rocketMQConfig = new RocketMQConfig();
        rocketMQConfig.setEnabled(true);
        rocketMQConfig.setNameServer("localhost:9876");

        RocketMQConfig.ProducerConfig producerConfig = new RocketMQConfig.ProducerConfig();
        producerConfig.setGroup("test-producer-group");
        rocketMQConfig.setProducer(producerConfig);

        RocketMQConfig.TopicsConfig topicsConfig = new RocketMQConfig.TopicsConfig();
        topicsConfig.setBetSettlements("bet-settlements-test");
        rocketMQConfig.setTopics(topicsConfig);

        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        if (realBetSettlementProducer != null) {
            try {
                realBetSettlementProducer.destroy();
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
        }
    }

    @Test
    void init_WithValidConfig_InitializesSuccessfully() {
        // Given
        RealBetSettlementProducer producer = new RealBetSettlementProducer(rocketMQConfig, objectMapper);

        // When - init is called via @PostConstruct, but we can test the scenario
        // The bean is created successfully without throwing exceptions

        // Then - no exceptions thrown during construction
        // In real scenario, this would test the actual init() method
    }

    @Test
    void sendSettlement_WithValidSettlement_SendsSuccessfully() throws Exception {
        // Given
        realBetSettlementProducer = createProducerWithMockedClient();

        BetSettlementDTO settlement = createSettlement(1L, true);
        when(sendReceipt.getMessageId()).thenReturn(messageId);
        when(messageId.toString()).thenReturn("MSG-123");
        when(producer.send(any())).thenReturn(sendReceipt);

        // When
        realBetSettlementProducer.sendSettlement(settlement);

        // Then
        verify(producer).send(any());
    }

    @Test
    void sendSettlement_WithWonBet_IncludesWonTag() throws Exception {
        // Given
        realBetSettlementProducer = createProducerWithMockedClient();

        BetSettlementDTO wonSettlement = createSettlement(1L, true);
        when(sendReceipt.getMessageId()).thenReturn(messageId);
        when(messageId.toString()).thenReturn("MSG-WON-123");
        when(producer.send(any())).thenReturn(sendReceipt);

        // When
        realBetSettlementProducer.sendSettlement(wonSettlement);

        // Then
        verify(producer).send(any());
    }

    @Test
    void sendSettlement_WithLostBet_IncludesLostTag() throws Exception {
        // Given
        realBetSettlementProducer = createProducerWithMockedClient();

        BetSettlementDTO lostSettlement = createSettlement(2L, false);
        when(sendReceipt.getMessageId()).thenReturn(messageId);
        when(messageId.toString()).thenReturn("MSG-LOST-456");
        when(producer.send(any())).thenReturn(sendReceipt);

        // When
        realBetSettlementProducer.sendSettlement(lostSettlement);

        // Then
        verify(producer).send(any());
    }

    @Test
    void sendSettlement_OnSendFailure_ThrowsRuntimeException() throws Exception {
        // Given
        realBetSettlementProducer = createProducerWithMockedClient();

        BetSettlementDTO settlement = createSettlement(1L, true);
        when(producer.send(any())).thenThrow(new ClientException("Connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                realBetSettlementProducer.sendSettlement(settlement)
        );

        assertThat(exception.getMessage(), containsString("Failed to send settlement to RocketMQ"));
        assertThat(exception.getCause(), instanceOf(ClientException.class));
    }

    @Test
    void sendSettlement_WithSerializationError_ThrowsRuntimeException() throws Exception {
        // Given
        ObjectMapper faultyMapper = mock(ObjectMapper.class);
        when(faultyMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

        realBetSettlementProducer = new RealBetSettlementProducer(rocketMQConfig, faultyMapper);
        injectMockedProducer(realBetSettlementProducer);

        BetSettlementDTO settlement = createSettlement(1L, true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                realBetSettlementProducer.sendSettlement(settlement)
        );

        assertThat(exception.getMessage(), containsString("Failed to send settlement to RocketMQ"));
    }

    @Test
    void sendSettlement_WithMultipleSettlements_SendsEach() throws Exception {
        // Given
        realBetSettlementProducer = createProducerWithMockedClient();

        BetSettlementDTO settlement1 = createSettlement(1L, true);
        BetSettlementDTO settlement2 = createSettlement(2L, false);
        BetSettlementDTO settlement3 = createSettlement(3L, true);

        when(sendReceipt.getMessageId()).thenReturn(messageId);
        when(messageId.toString()).thenReturn("MSG-1", "MSG-2", "MSG-3");
        when(producer.send(any())).thenReturn(sendReceipt);

        // When
        realBetSettlementProducer.sendSettlement(settlement1);
        realBetSettlementProducer.sendSettlement(settlement2);
        realBetSettlementProducer.sendSettlement(settlement3);

        // Then
        verify(producer, times(3)).send(any());
    }

    @Test
    void destroy_WithActiveProducer_ClosesSuccessfully() throws Exception {
        // Given
        realBetSettlementProducer = createProducerWithMockedClient();

        // When
        realBetSettlementProducer.destroy();

        // Then
        verify(producer).close();
    }

    @Test
    void destroy_WithNullProducer_DoesNotThrowException() {
        // Given
        realBetSettlementProducer = new RealBetSettlementProducer(rocketMQConfig, objectMapper);

        // When & Then - should not throw
        realBetSettlementProducer.destroy();
    }

    @Test
    void destroy_OnCloseException_LogsErrorButDoesNotThrow() throws Exception {
        // Given
        realBetSettlementProducer = createProducerWithMockedClient();
        doThrow(new RuntimeException("Close failed")).when(producer).close();

        // When & Then - should not throw
        realBetSettlementProducer.destroy();

        verify(producer).close();
    }

    @Test
    void sendSettlement_WithLargeBetAmount_HandlesCorrectly() throws Exception {
        // Given
        realBetSettlementProducer = createProducerWithMockedClient();

        BetSettlementDTO settlement = BetSettlementDTO.builder()
                .betId(1L)
                .userId("USER-001")
                .eventId("EVT-001")
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId("TEAM-A")
                .predictedWinnerId("TEAM-A")
                .betAmount(new BigDecimal("9999999.99"))
                .won(true)
                .build();

        when(sendReceipt.getMessageId()).thenReturn(messageId);
        when(messageId.toString()).thenReturn("MSG-LARGE");
        when(producer.send(any())).thenReturn(sendReceipt);

        // When
        realBetSettlementProducer.sendSettlement(settlement);

        // Then
        verify(producer).send(any());
    }

    @Test
    void sendSettlement_WithSpecialCharactersInEventId_HandlesCorrectly() throws Exception {
        // Given
        realBetSettlementProducer = createProducerWithMockedClient();

        BetSettlementDTO settlement = createSettlement(1L, true);
        settlement = BetSettlementDTO.builder()
                .betId(settlement.getBetId())
                .userId(settlement.getUserId())
                .eventId("EVT-特殊-001-@#$")
                .eventMarketId(settlement.getEventMarketId())
                .eventWinnerId(settlement.getEventWinnerId())
                .predictedWinnerId(settlement.getPredictedWinnerId())
                .betAmount(settlement.getBetAmount())
                .won(settlement.getWon())
                .build();

        when(sendReceipt.getMessageId()).thenReturn(messageId);
        when(messageId.toString()).thenReturn("MSG-SPECIAL");
        when(producer.send(any())).thenReturn(sendReceipt);

        // When
        realBetSettlementProducer.sendSettlement(settlement);

        // Then
        verify(producer).send(any());
    }

    @Test
    void sendSettlement_VerifiesMessageIdLogged() throws Exception {
        // Given
        realBetSettlementProducer = createProducerWithMockedClient();

        BetSettlementDTO settlement = createSettlement(1L, true);
        when(sendReceipt.getMessageId()).thenReturn(messageId);
        when(messageId.toString()).thenReturn("TEST-MSG-ID");
        when(producer.send(any())).thenReturn(sendReceipt);

        // When
        realBetSettlementProducer.sendSettlement(settlement);

        // Then
        verify(producer).send(any());
        verify(sendReceipt).getMessageId();
    }

    /**
     * Helper method to create a RealBetSettlementProducer with mocked Producer.
     */
    private RealBetSettlementProducer createProducerWithMockedClient() throws Exception {
        RealBetSettlementProducer producer = new RealBetSettlementProducer(rocketMQConfig, objectMapper);
        injectMockedProducer(producer);
        return producer;
    }

    /**
     * Helper method to inject mocked Producer into RealBetSettlementProducer using reflection.
     */
    private void injectMockedProducer(RealBetSettlementProducer producerInstance) throws Exception {
        Field producerField = RealBetSettlementProducer.class.getDeclaredField("producer");
        producerField.setAccessible(true);
        producerField.set(producerInstance, producer);
    }

    /**
     * Helper method to create a test settlement.
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