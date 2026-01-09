package com.microservices.notification.kafka;

import com.microservices.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.topics.order-events:order.events}", groupId = "notification-service")
    public void onOrderEvent(@Payload OrderEvent event) {
        log.info("Received order event {}", event.getOrderId());
        notificationService.saveFromEvent(event);
    }
}
