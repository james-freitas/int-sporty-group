package com.sportygroup.betsettler.kafka;

import com.sportygroup.betsettler.config.KafkaTopicConfig;
import com.sportygroup.betsettler.dto.EventOutcomeDTO;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventOutcomeProducer.
 *
 * Tests the Kafka message sending logic including:
 * - Successful message sending
 * - Error handling
 * - Proper topic and key usage
 * - Callback handling
 */
@ExtendWith(MockitoExtension.class)
class EventOutcomeProducerTest {

    @Mock
    private KafkaTemplate<String, EventOutcomeDTO> kafkaTemplate;

    @Mock
    private KafkaTopicConfig topicConfig;

    @InjectMocks
    private EventOutcomeProducer eventOutcomeProducer;

    private EventOutcomeDTO eventOutcome;
    private String topicName;

    @BeforeEach
    void setUp() {
        topicName = "event-outcomes";

        eventOutcome = EventOutcomeDTO.builder()
                .eventId("EVT-001")
                .eventName("Test Match")
                .eventWinnerId("TEAM-A")
                .build();

        when(topicConfig.getEventOutcomes()).thenReturn(topicName);
    }

    @Test
    void sendEventOutcome_WithValidEvent_SendsToCorrectTopic() {
        // Given
        CompletableFuture<SendResult<String, EventOutcomeDTO>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(topicName), eq("EVT-001"), eq(eventOutcome)))
                .thenReturn(future);

        // When
        eventOutcomeProducer.sendEventOutcome(eventOutcome);

        // Then
        verify(kafkaTemplate).send(topicName, "EVT-001", eventOutcome);
        verify(topicConfig).getEventOutcomes();
    }

    @Test
    void sendEventOutcome_UsesEventIdAsKey() {
        // Given
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        CompletableFuture<SendResult<String, EventOutcomeDTO>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(topicName), anyString(), eq(eventOutcome)))
                .thenReturn(future);

        // When
        eventOutcomeProducer.sendEventOutcome(eventOutcome);

        // Then
        verify(kafkaTemplate).send(eq(topicName), keyCaptor.capture(), eq(eventOutcome));
        assertThat(keyCaptor.getValue(), equalTo("EVT-001"));
    }

    @Test
    void sendEventOutcome_OnFailure_LogsErrorMessage() {
        // Given
        RuntimeException expectedException = new RuntimeException("Kafka connection failed");
        CompletableFuture<SendResult<String, EventOutcomeDTO>> future = new CompletableFuture<>();
        future.completeExceptionally(expectedException);

        when(kafkaTemplate.send(eq(topicName), eq("EVT-001"), eq(eventOutcome)))
                .thenReturn(future);

        // When
        eventOutcomeProducer.sendEventOutcome(eventOutcome);

        // Then
        verify(kafkaTemplate).send(topicName, "EVT-001", eventOutcome);

        // Wait for async callback to complete
        try {
            future.join();
        } catch (Exception e) {
            // Expected - the future completed exceptionally
            assertThat(e.getCause(), instanceOf(RuntimeException.class));
            assertThat(e.getCause().getMessage(), containsString("Kafka connection failed"));
        }
    }

    @Test
    void sendEventOutcome_WithDifferentEventIds_UsesDifferentKeys() {
        // Given
        EventOutcomeDTO event1 = EventOutcomeDTO.builder()
                .eventId("EVT-001")
                .eventName("Match 1")
                .eventWinnerId("TEAM-A")
                .build();

        EventOutcomeDTO event2 = EventOutcomeDTO.builder()
                .eventId("EVT-002")
                .eventName("Match 2")
                .eventWinnerId("TEAM-B")
                .build();

        CompletableFuture<SendResult<String, EventOutcomeDTO>> future1 = new CompletableFuture<>();
        CompletableFuture<SendResult<String, EventOutcomeDTO>> future2 = new CompletableFuture<>();

        when(kafkaTemplate.send(eq(topicName), eq("EVT-001"), eq(event1)))
                .thenReturn(future1);
        when(kafkaTemplate.send(eq(topicName), eq("EVT-002"), eq(event2)))
                .thenReturn(future2);

        // When
        eventOutcomeProducer.sendEventOutcome(event1);
        eventOutcomeProducer.sendEventOutcome(event2);

        // Then
        verify(kafkaTemplate).send(topicName, "EVT-001", event1);
        verify(kafkaTemplate).send(topicName, "EVT-002", event2);
    }

    @Test
    void sendEventOutcome_WithNullEventId_StillAttemptsSend() {
        // Given
        EventOutcomeDTO eventWithNullId = EventOutcomeDTO.builder()
                .eventId(null)
                .eventName("Test Match")
                .eventWinnerId("TEAM-A")
                .build();

        CompletableFuture<SendResult<String, EventOutcomeDTO>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(topicName), isNull(), eq(eventWithNullId)))
                .thenReturn(future);

        // When
        eventOutcomeProducer.sendEventOutcome(eventWithNullId);

        // Then
        verify(kafkaTemplate).send(eq(topicName), isNull(), eq(eventWithNullId));
    }

    @Test
    void sendEventOutcome_RetrievesTopicFromConfig() {
        // Given
        CompletableFuture<SendResult<String, EventOutcomeDTO>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any(EventOutcomeDTO.class)))
                .thenReturn(future);

        // When
        eventOutcomeProducer.sendEventOutcome(eventOutcome);

        // Then
        verify(topicConfig, times(1)).getEventOutcomes();
    }

    @Test
    void sendEventOutcome_MultipleCalls_SendsEachMessage() {
        // Given
        CompletableFuture<SendResult<String, EventOutcomeDTO>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(topicName), anyString(), any(EventOutcomeDTO.class)))
                .thenReturn(future);

        // When
        eventOutcomeProducer.sendEventOutcome(eventOutcome);
        eventOutcomeProducer.sendEventOutcome(eventOutcome);
        eventOutcomeProducer.sendEventOutcome(eventOutcome);

        // Then
        verify(kafkaTemplate, times(3)).send(eq(topicName), eq("EVT-001"), eq(eventOutcome));
    }

    @Test
    void sendEventOutcome_WithComplexEventName_SendsSuccessfully() {
        // Given
        EventOutcomeDTO complexEvent = EventOutcomeDTO.builder()
                .eventId("EVT-COMPLEX-123")
                .eventName("Champions League Final 2024: Team A vs Team B")
                .eventWinnerId("TEAM-A-ID-456")
                .build();

        CompletableFuture<SendResult<String, EventOutcomeDTO>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(topicName), eq("EVT-COMPLEX-123"), eq(complexEvent)))
                .thenReturn(future);

        // When
        eventOutcomeProducer.sendEventOutcome(complexEvent);

        // Then
        verify(kafkaTemplate).send(topicName, "EVT-COMPLEX-123", complexEvent);
    }

    @Test
    void sendEventOutcome_CallbackHandling_CompletesSuccessfully() throws Exception {
        // Given
        SendResult<String, EventOutcomeDTO> sendResult = createSuccessfulSendResult();
        CompletableFuture<SendResult<String, EventOutcomeDTO>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(topicName), eq("EVT-001"), eq(eventOutcome)))
                .thenReturn(future);

        // When
        eventOutcomeProducer.sendEventOutcome(eventOutcome);

        // Then
        SendResult<String, EventOutcomeDTO> result = future.get();
        assertThat(result, is(notNullValue()));
        assertThat(result.getRecordMetadata(), is(notNullValue()));
        assertThat(result.getRecordMetadata().topic(), equalTo(topicName));
    }

    @Test
    void sendEventOutcome_AsyncCallback_HandlesFailure() {
        // Given
        RuntimeException exception = new RuntimeException("Network error");
        CompletableFuture<SendResult<String, EventOutcomeDTO>> future = new CompletableFuture<>();

        when(kafkaTemplate.send(eq(topicName), eq("EVT-001"), eq(eventOutcome)))
                .thenReturn(future);

        // When
        eventOutcomeProducer.sendEventOutcome(eventOutcome);

        // Simulate failure
        future.completeExceptionally(exception);

        // Then
        assertThat(future.isDone(), is(true));
        assertThat(future.isCompletedExceptionally(), is(true));

        try {
            future.join();
        } catch (Exception e) {
            assertThat(e.getCause(), equalTo(exception));
        }
    }

    /**
     * Helper method to create a successful SendResult for testing.
     */
    private SendResult<String, EventOutcomeDTO> createSuccessfulSendResult() {
        ProducerRecord<String, EventOutcomeDTO> producerRecord =
                new ProducerRecord<>(topicName, 0, "EVT-001", eventOutcome);

        TopicPartition topicPartition = new TopicPartition(topicName, 0);
        RecordMetadata recordMetadata = new RecordMetadata(
                topicPartition,
                0L,      // baseOffset
                0,       // batchIndex
                System.currentTimeMillis(),
                123L,    // offset
                -1,      // serializedKeySize
                -1       // serializedValueSize
        );

        return new SendResult<>(producerRecord, recordMetadata);
    }
}