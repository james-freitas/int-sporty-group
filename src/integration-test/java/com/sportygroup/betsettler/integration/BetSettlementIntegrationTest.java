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
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * Integration test using Embedded Kafka (no Docker required).
 *
 * This is an alternative to Testcontainers-based tests and works
 * without Docker installation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        topics = {"${application.kafka.topics.event-outcomes}"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093",
                "port=9093"
        }
)
@TestPropertySource(
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "application.rocketmq.enabled=false"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BetSettlementEmbeddedKafkaIntegrationTest {

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
        Bet winningBet = createBet("USER-001", "EVT-EMBEDDED-001", "TEAM-A", "100.00");
        Bet losingBet = createBet("USER-002", "EVT-EMBEDDED-001", "TEAM-B", "50.00");

        betRepository.save(winningBet);
        betRepository.save(losingBet);

        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("EVT-EMBEDDED-001")
                .eventName("Embedded Kafka Test Match")
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
                    List<Bet> settledBets = betRepository.findByEventId("EVT-EMBEDDED-001");

                    assertThat(settledBets).hasSize(2);
                    assertThat(settledBets).allMatch(bet -> bet.getSettledAt() != null);

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
        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("EVT-NO-BETS-EMBEDDED")
                .eventName("Event With No Bets")
                .eventWinnerId("TEAM-X")
                .build();

        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        Thread.sleep(2000);

        List<Bet> bets = betRepository.findByEventId("EVT-NO-BETS-EMBEDDED");
        assertThat(bets).isEmpty();
    }

    @Test
    void publishEventOutcome_WithMultipleBets_SettlesAllCorrectly() throws Exception {
        betRepository.save(createBet("USER-001", "EVT-MULTI-EMBEDDED", "WINNER-A", "100.00"));
        betRepository.save(createBet("USER-002", "EVT-MULTI-EMBEDDED", "WINNER-A", "50.00"));
        betRepository.save(createBet("USER-003", "EVT-MULTI-EMBEDDED", "WINNER-B", "75.00"));
        betRepository.save(createBet("USER-004", "EVT-MULTI-EMBEDDED", "WINNER-B", "25.00"));

        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("EVT-MULTI-EMBEDDED")
                .eventName("Multiple Bets Test")
                .eventWinnerId("WINNER-A")
                .build();

        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<Bet> wonBets = betRepository.findByEventIdAndStatus("EVT-MULTI-EMBEDDED", BetStatus.WON);
                    List<Bet> lostBets = betRepository.findByEventIdAndStatus("EVT-MULTI-EMBEDDED", BetStatus.LOST);

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