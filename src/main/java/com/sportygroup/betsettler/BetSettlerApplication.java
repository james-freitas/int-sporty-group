package com.sportygroup.betsettler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application for Sports Betting Settlement Service.
 *
 * This service handles:
 * - Event outcome publishing via REST API
 * - Kafka-based event streaming
 * - Bet matching and settlement processing
 * - RocketMQ-based settlement message distribution
 *
 * @author James
 * @version 1.0.0
 */
@SpringBootApplication
public class BetSettlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BetSettlerApplication.class, args);
	}

}
