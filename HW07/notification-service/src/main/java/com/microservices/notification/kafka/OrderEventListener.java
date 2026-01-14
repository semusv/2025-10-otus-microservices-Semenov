package com.microservices.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.notification.service.NotificationService;
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
public class OrderEventListener {

    private final NotificationService notificationService;

    private final ObjectMapper objectMapper;

    //    @KafkaListener(topics = "${app.kafka.topics.order-events:order.events}", groupId = "notification-service")
    //    public void onOrderEvent(@Payload OrderEvent event) {
    //        log.info("Received order event {}", event.getOrderId());
    //        notificationService.saveFromEvent(event);
    //    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-events:order.events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleUserCreated(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        try {
            log.info("Received order event. Key: {}, Topic: {}", key, topic);
            // Конвертируем Map в DTO
            OrderEvent event = objectMapper.convertValue(payload, OrderEvent.class);

            // Обрабатываем событие
            notificationService.saveFromEvent(event);

            log.info("Successfully processed order event for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error processing order event. Payload: {}", payload, e);
            throw e;
        }
    }
}
