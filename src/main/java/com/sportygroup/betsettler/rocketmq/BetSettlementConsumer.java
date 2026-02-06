package com.sportygroup.betsettler.rocketmq;

import com.sportygroup.betsettler.config.RocketMQConfig;
import com.sportygroup.betsettler.dto.BetSettlementDTO;
import com.sportygroup.betsettler.service.BetSettlementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * RocketMQ consumer for bet settlement messages.
 *
 * Listens to the bet-settlements topic and processes each
 * settlement by updating the bet status in the database.
 *
 * Only active when application.rocketmq.enabled=true
 */
@Component
@ConditionalOnProperty(name = "application.rocketmq.enabled", havingValue = "true")
@Slf4j
public class BetSettlementConsumer {

    private final RocketMQConfig rocketMQConfig;
    private final BetSettlementService betSettlementService;
    private final ObjectMapper objectMapper;
    private PushConsumer consumer;

    public BetSettlementConsumer(
            RocketMQConfig rocketMQConfig,
            BetSettlementService betSettlementService,
            ObjectMapper objectMapper) {
        this.rocketMQConfig = rocketMQConfig;
        this.betSettlementService = betSettlementService;
        this.objectMapper = objectMapper;
    }

    /**
     * Initializes RocketMQ consumer on bean creation.
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing RocketMQ Consumer - NameServer: {}, Group: {}",
                    rocketMQConfig.getNameServer(),
                    rocketMQConfig.getConsumer().getGroup());

            ClientServiceProvider provider = ClientServiceProvider.loadService();

            ClientConfiguration clientConfig = ClientConfiguration.newBuilder()
                    .setEndpoints(rocketMQConfig.getNameServer())
                    .build();

            // Subscribe to all tags
            FilterExpression filterExpression = new FilterExpression(
                    "*",
                    FilterExpressionType.TAG
            );

            this.consumer = provider.newPushConsumerBuilder()
                    .setClientConfiguration(clientConfig)
                    .setConsumerGroup(rocketMQConfig.getConsumer().getGroup())
                    .setSubscriptionExpressions(Collections.singletonMap(
                            rocketMQConfig.getTopics().getBetSettlements(),
                            filterExpression
                    ))
                    .setMessageListener(messageView -> {
                        try {
                            // Deserialize message
                            String json = StandardCharsets.UTF_8.decode(
                                    messageView.getBody()
                            ).toString();

                            BetSettlementDTO settlement = objectMapper.readValue(
                                    json,
                                    BetSettlementDTO.class
                            );

                            log.info("Received bet settlement from RocketMQ - Bet ID: {}, Message ID: {}",
                                    settlement.getBetId(), messageView.getMessageId());

                            // Process settlement
                            betSettlementService.settleBet(settlement);

                            log.info("Successfully processed bet settlement - Bet ID: {}",
                                    settlement.getBetId());

                            return ConsumeResult.SUCCESS;

                        } catch (Exception e) {
                            log.error("Error processing bet settlement message - Message ID: {}, Error: {}",
                                    messageView.getMessageId(), e.getMessage(), e);
                            return ConsumeResult.FAILURE;
                        }
                    })
                    .build();

            log.info("RocketMQ Consumer initialized and listening for messages");

        } catch (ClientException e) {
            log.error("Failed to initialize RocketMQ Consumer", e);
            throw new RuntimeException("Failed to initialize RocketMQ Consumer", e);
        }
    }

    /**
     * Gracefully shuts down RocketMQ consumer.
     */
    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            try {
                log.info("Shutting down RocketMQ Consumer");
                consumer.close();
                log.info("RocketMQ Consumer shut down successfully");
            } catch (Exception e) {
                log.error("Error shutting down RocketMQ Consumer", e);
            }
        }
    }
}
