package com.sportygroup.betsettler.rocketmq;

import com.sportygroup.betsettler.config.RocketMQConfig;
import com.sportygroup.betsettler.dto.BetSettlementDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

/**
 * Real RocketMQ implementation of BetSettlementProducer.
 *
 * Sends bet settlement messages to RocketMQ broker.
 *
 * Activated when application.rocketmq.enabled=true
 */
@Component
@ConditionalOnProperty(name = "application.rocketmq.enabled", havingValue = "true")
@Slf4j
public class RealBetSettlementProducer implements BetSettlementProducer {

    private final RocketMQConfig rocketMQConfig;
    private final ObjectMapper objectMapper;
    private Producer producer;

    public RealBetSettlementProducer(RocketMQConfig rocketMQConfig, ObjectMapper objectMapper) {
        this.rocketMQConfig = rocketMQConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Initializes RocketMQ producer on bean creation.
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing RocketMQ Producer - NameServer: {}, Group: {}",
                    rocketMQConfig.getNameServer(),
                    rocketMQConfig.getProducer().getGroup());

            ClientServiceProvider provider = ClientServiceProvider.loadService();

            ClientConfiguration clientConfig = ClientConfiguration.newBuilder()
                    .setEndpoints(rocketMQConfig.getNameServer())
                    .build();

            this.producer = provider.newProducerBuilder()
                    .setClientConfiguration(clientConfig)
                    .setTopics(rocketMQConfig.getTopics().getBetSettlements())
                    .build();

            log.info("RocketMQ Producer initialized successfully");
        } catch (ClientException e) {
            log.error("Failed to initialize RocketMQ Producer", e);
            throw new RuntimeException("Failed to initialize RocketMQ Producer", e);
        }
    }

    /**
     * Sends a bet settlement message to RocketMQ.
     *
     * @param settlement The bet settlement to send
     */
    @Override
    public void sendSettlement(BetSettlementDTO settlement) {
        try {
            log.info("Sending bet settlement to RocketMQ - Bet ID: {}, Event: {}",
                    settlement.getBetId(), settlement.getEventId());

            // Serialize settlement to JSON
            String json = objectMapper.writeValueAsString(settlement);
            byte[] messageBody = json.getBytes(StandardCharsets.UTF_8);

            // Create message with bet ID as key
            Message message = provider.newMessageBuilder()
                    .setTopic(rocketMQConfig.getTopics().getBetSettlements())
                    .setKeys(settlement.getBetId().toString())
                    .setTag(settlement.getWon() ? "WON" : "LOST")
                    .setBody(messageBody)
                    .build();

            // Send message
            SendReceipt sendReceipt = producer.send(message);

            log.info("Bet settlement sent successfully - Bet ID: {}, Message ID: {}",
                    settlement.getBetId(), sendReceipt.getMessageId());

        } catch (Exception e) {
            log.error("Failed to send bet settlement to RocketMQ - Bet ID: {}",
                    settlement.getBetId(), e);
            throw new RuntimeException("Failed to send settlement to RocketMQ", e);
        }
    }

    /**
     * Gracefully shuts down RocketMQ producer.
     */
    @PreDestroy
    public void destroy() {
        if (producer != null) {
            try {
                log.info("Shutting down RocketMQ Producer");
                producer.close();
                log.info("RocketMQ Producer shut down successfully");
            } catch (Exception e) {
                log.error("Error shutting down RocketMQ Producer", e);
            }
        }
    }

    private static final ClientServiceProvider provider = ClientServiceProvider.loadService();
}
