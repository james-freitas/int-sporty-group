package com.sportygroup.betsettler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Kafka topics.
 *
 * Binds properties from application.yml with prefix 'application.kafka.topics'.
 */
@Configuration
@ConfigurationProperties(prefix = "application.kafka.topics")
@Data
public class KafkaTopicConfig {

    /**
     * Topic name for event outcomes
     */
    private String eventOutcomes = "event-outcomes";
}