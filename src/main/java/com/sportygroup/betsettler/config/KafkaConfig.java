package com.sportygroup.betsettler.config;

import com.sportygroup.betsettler.dto.EventOutcomeDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Apache Kafka.
 *
 * Configures Kafka producers and consumers with JSON serialization.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Configuration for Kafka Producer.
     *
     * Produces EventOutcomeDTO messages to Kafka topics.
     */
    @Bean
    public ProducerFactory<String, EventOutcomeDTO> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate for sending messages to Kafka.
     */
    @Bean
    public KafkaTemplate<String, EventOutcomeDTO> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Configuration for Kafka Consumer.
     *
     * Consumes EventOutcomeDTO messages from Kafka topics.
     */
    @Bean
    public ConsumerFactory<String, EventOutcomeDTO> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.betting.settlement.dto");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EventOutcomeDTO.class.getName());
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(
                configProps,
                new StringDeserializer(),
                new JsonDeserializer<>(EventOutcomeDTO.class, false)
        );
    }

    /**
     * Kafka Listener Container Factory for @KafkaListener annotations.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventOutcomeDTO> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventOutcomeDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);
        return factory;
    }
}
