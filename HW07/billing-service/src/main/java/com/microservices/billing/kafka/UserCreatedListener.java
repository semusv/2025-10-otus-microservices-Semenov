package com.microservices.billing.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.billing.service.AccountService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedListener {

    private final AccountService accountService;

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.user-created:user.created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleUserCreated(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        try {
            log.info("Received user.created event. Key: {}, Topic: {}", key, topic);
            // Конвертируем Map в DTO
            UserCreatedEvent event = objectMapper.convertValue(payload, UserCreatedEvent.class);

            // Обрабатываем событие
            accountService.createAccountIfMissing(event);

            log.info("Successfully processed user.created event for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error processing user.created event. Payload: {}", payload, e);
            throw e;
        }
    }
}
