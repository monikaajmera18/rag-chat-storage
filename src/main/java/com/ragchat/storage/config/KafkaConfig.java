package com.ragchat.storage.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.session-events}")
    private String sessionEventsTopic;

    @Value("${app.kafka.topics.message-events}")
    private String messageEventsTopic;

    @Bean
    public NewTopic sessionEventsTopic() {
        return TopicBuilder.name(sessionEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic messageEventsTopic() {
        return TopicBuilder.name(messageEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}