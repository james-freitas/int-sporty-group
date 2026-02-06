package com.sportygroup.betsettler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for RocketMQ.
 *
 * Binds properties from application.yml with prefix 'application.rocketmq'.
 */
@Configuration
@ConfigurationProperties(prefix = "application.rocketmq")
@Data
public class RocketMQConfig {

    /**
     * Whether RocketMQ is enabled (true) or mocked (false)
     */
    private boolean enabled = false;

    /**
     * RocketMQ NameServer address
     */
    private String nameServer = "localhost:9876";

    /**
     * Producer configuration
     */
    private ProducerConfig producer = new ProducerConfig();

    /**
     * Consumer configuration
     */
    private ConsumerConfig consumer = new ConsumerConfig();

    /**
     * Topic configuration
     */
    private TopicsConfig topics = new TopicsConfig();

    @Data
    public static class ProducerConfig {
        private String group = "betting-settlement-producer";
        private int sendMsgTimeout = 3000;
        private int retryTimesWhenSendFailed = 2;
    }

    @Data
    public static class ConsumerConfig {
        private String group = "betting-settlement-consumer";
        private int consumeThreadMin = 5;
        private int consumeThreadMax = 20;
    }

    @Data
    public static class TopicsConfig {
        private String betSettlements = "bet-settlements";
    }
}