package com.ragchat.storage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.session-events}")
    private String sessionEventsTopic;

    @Value("${app.kafka.topics.message-events}")
    private String messageEventsTopic;

    public void publishSessionEvent(String eventType, Long sessionId, String userId, String sessionName) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("sessionId", sessionId);
        event.put("userId", userId);
        event.put("sessionName", sessionName);
        event.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(sessionEventsTopic, sessionId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Session event published: {} for session {}", eventType, sessionId);
                    } else {
                        log.error("Failed to publish session event: {}", eventType, ex);
                    }
                });
    }

    public void publishMessageEvent(String eventType, Long messageId, Long sessionId, String sender, String content) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("messageId", messageId);
        event.put("sessionId", sessionId);
        event.put("sender", sender);
        event.put("contentLength", content.length());
        event.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(messageEventsTopic, messageId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Message event published: {} for message {}", eventType, messageId);
                    } else {
                        log.error("Failed to publish message event: {}", eventType, ex);
                    }
                });
    }
}