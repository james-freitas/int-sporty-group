package com.sportygroup.betsettler.repository;

import com.sportygroup.betsettler.entity.Bet;
import com.sportygroup.betsettler.entity.BetStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for BetRepository.
 */
@DataJpaTest
class BetRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BetRepository betRepository;

    private Bet pendingBet1;
    private Bet pendingBet2;
    private Bet settledBet;

    @BeforeEach
    void setUp() {
        betRepository.deleteAll();
        pendingBet1 = Bet.builder()
                .userId("USER-001")
                .eventId("EVT-001")
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId("TEAM-A")
                .betAmount(new BigDecimal("100.00"))
                .status(BetStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        pendingBet2 = Bet.builder()
                .userId("USER-002")
                .eventId("EVT-001")
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId("TEAM-B")
                .betAmount(new BigDecimal("50.00"))
                .status(BetStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        settledBet = Bet.builder()
                .userId("USER-003")
                .eventId("EVT-001")
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId("TEAM-A")
                .betAmount(new BigDecimal("75.00"))
                .status(BetStatus.WON)
                .createdAt(LocalDateTime.now().minusHours(2))
                .settledAt(LocalDateTime.now().minusHours(1))
                .build();

        entityManager.persist(pendingBet1);
        entityManager.persist(pendingBet2);
        entityManager.persist(settledBet);
        entityManager.flush();
    }

    @Test
    void findByEventIdAndStatus_WithPendingStatus_ReturnsPendingBets() {
        // When
        List<Bet> results = betRepository.findByEventIdAndStatus("EVT-001", BetStatus.PENDING);

        // Then
        assertThat(results, hasSize(2));
        assertThat(results, everyItem(hasProperty("status", equalTo(BetStatus.PENDING))));
        assertThat(results, everyItem(hasProperty("eventId", equalTo("EVT-001"))));
    }

    @Test
    void findByEventIdAndStatus_WithWonStatus_ReturnsSettledBets() {
        // When
        List<Bet> results = betRepository.findByEventIdAndStatus("EVT-001", BetStatus.WON);

        // Then
        assertThat(results, hasSize(1));
        assertThat(results.get(0).getStatus(), equalTo(BetStatus.WON));
        assertThat(results.get(0).getSettledAt(), is(notNullValue()));
    }

    @Test
    void findByEventIdAndStatus_WithNonExistentEvent_ReturnsEmpty() {
        // When
        List<Bet> results = betRepository.findByEventIdAndStatus("EVT-999", BetStatus.PENDING);

        // Then
        assertThat(results, is(empty()));
    }

    @Test
    void findByUserId_ReturnsUserBets() {
        // When
        List<Bet> results = betRepository.findByUserId("USER-001");

        // Then
        assertThat(results, hasSize(1));
        assertThat(results.get(0).getUserId(), equalTo("USER-001"));
    }

    @Test
    void findByEventId_ReturnsAllBetsForEvent() {
        // When
        List<Bet> results = betRepository.findByEventId("EVT-001");

        // Then
        assertThat(results, hasSize(3));
        assertThat(results, everyItem(hasProperty("eventId", equalTo("EVT-001"))));
    }

    @Test
    void findByStatus_ReturnsBetsWithStatus() {
        // When
        List<Bet> results = betRepository.findByStatus(BetStatus.PENDING);

        // Then
        assertThat(results, hasSize(2));
        assertThat(results, everyItem(hasProperty("status", equalTo(BetStatus.PENDING))));
    }

    @Test
    void save_PersistsBet() {
        // Given
        Bet newBet = Bet.builder()
                .userId("USER-004")
                .eventId("EVT-002")
                .eventMarketId("OVER_UNDER")
                .eventWinnerId("OVER")
                .betAmount(new BigDecimal("200.00"))
                .status(BetStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        Bet saved = betRepository.save(newBet);

        // Then
        assertThat(saved.getBetId(), is(notNullValue()));
        assertThat(saved.getUserId(), equalTo("USER-004"));

        Bet found = betRepository.findById(saved.getBetId()).orElse(null);
        assertThat(found, is(notNullValue()));
        assertThat(found.getUserId(), equalTo("USER-004"));
    }

    @Test
    void betEntity_PrePersist_SetsCreatedAt() {
        // Given
        Bet betWithoutTimestamp = Bet.builder()
                .userId("USER-005")
                .eventId("EVT-003")
                .eventMarketId("MATCH_WINNER")
                .eventWinnerId("TEAM-A")
                .betAmount(new BigDecimal("150.00"))
                .status(BetStatus.PENDING)
                .build();

        // When
        Bet saved = betRepository.save(betWithoutTimestamp);

        // Then
        assertThat(saved.getCreatedAt(), is(notNullValue()));
    }
}