package com.sportygroup.betsettler.integration;

import com.sportygroup.betsettler.dto.PublishEventRequest;
import com.sportygroup.betsettler.entity.Bet;
import com.sportygroup.betsettler.entity.BetStatus;
import com.sportygroup.betsettler.repository.BetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.Duration;

/**
 * End-to-end integration test for the complete bet settlement flow.
 *
 * Tests the full flow from API call through Kafka to database updates.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class BetSettlementIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("application.rocketmq.enabled", () -> "false"); // Use mock
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BetRepository betRepository;

    @BeforeEach
    void setUp() {
        betRepository.deleteAll();
    }

    @Test
    void completeFlow_PublishEventOutcome_SettlesBets() throws Exception {
        // Given: Create pending bets in database
        Bet winningBet = createBet("USER-001", "EVT-INTEGRATION-001", "TEAM-A", "100.00");
        Bet losingBet = createBet("USER-002", "EVT-INTEGRATION-001", "TEAM-B", "50.00");

        betRepository.save(winningBet);
        betRepository.save(losingBet);

        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("EVT-INTEGRATION-001")
                .eventName("Integration Test Match")
                .eventWinnerId("TEAM-A")
                .build();

        // When: Publish event outcome via API
        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        // Then: Wait for async processing and verify bets are settled
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<Bet> settledBets = betRepository.findByEventId("EVT-INTEGRATION-001");

                    assertThat(settledBets).hasSize(2);
                    assertThat(settledBets).allMatch(bet -> bet.getSettledAt() != null);

                    // Find the winning and losing bet
                    Bet wonBet = settledBets.stream()
                            .filter(b -> b.getUserId().equals("USER-001"))
                            .findFirst()
                            .orElseThrow();

                    Bet lostBet = settledBets.stream()
                            .filter(b -> b.getUserId().equals("USER-002"))
                            .findFirst()
                            .orElseThrow();

                    assertThat(wonBet.getStatus()).isEqualTo(BetStatus.WON);
                    assertThat(lostBet.getStatus()).isEqualTo(BetStatus.LOST);
                });
    }

    @Test
    void publishEventOutcome_WithNoPendingBets_ProcessesWithoutError() throws Exception {
        // Given: No pending bets for this event
        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("EVT-NO-BETS")
                .eventName("Event With No Bets")
                .eventWinnerId("TEAM-X")
                .build();

        // When & Then: Should process without error
        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        // Give it time to process
        Thread.sleep(2000);

        // Verify no errors occurred (test passes if no exception thrown)
        List<Bet> bets = betRepository.findByEventId("EVT-NO-BETS");
        assertThat(bets).isEmpty();
    }

    @Test
    void publishEventOutcome_WithMultipleBets_SettlesAllCorrectly() throws Exception {
        // Given: Multiple bets with different predictions
        betRepository.save(createBet("USER-001", "EVT-MULTI", "WINNER-A", "100.00"));
        betRepository.save(createBet("USER-002", "EVT-MULTI", "WINNER-A", "50.00"));
        betRepository.save(createBet("USER-003", "EVT-MULTI", "WINNER-B", "75.00"));
        betRepository.save(createBet("USER-004", "EVT-MULTI", "WINNER-B", "25.00"));

        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("EVT-MULTI")
                .eventName("Multiple Bets Test")
                .eventWinnerId("WINNER-A")
                .build();

        // When
        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        // Then
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<Bet> allBets = betRepository.findByEventId("EVT-MULTI");

                    assertThat(allBets).hasSize(4);

                    List<Bet> wonBets = betRepository.findByEventIdAndStatus("EVT-MULTI", BetStatus.WON);
                    List<Bet> lostBets = betRepository.findByEventIdAndStatus("EVT-MULTI", BetStatus.LOST);

                    assertThat(wonBets).hasSize(2);
                    assertThat(lostBets).hasSize(2);

                    assertThat(wonBets).allMatch(b -> b.getEventWinnerId().equals("WINNER-A"));
                    assertThat(lostBets).allMatch(b -> b.getEventWinnerId().equals("WINNER-B"));
                });
    }

    private Bet createBet(String userId, String eventId, String winnerId, String amount) {
        return Bet.builder()
                .userId(userId)
                .eventId(eventId)
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId(winnerId)
                .betAmount(new BigDecimal(amount))
                .status(BetStatus.PENDING)
                .build();
    }
}