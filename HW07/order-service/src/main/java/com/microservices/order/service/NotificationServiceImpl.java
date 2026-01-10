package com.microservices.order.service;

import com.microservices.order.client.UserServiceClient;
import com.microservices.order.kafka.OrderEvent;
import com.microservices.order.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private final UserServiceClient userClient;

    @Value("${app.kafka.topics.order-events:order.events}")
    private String orderTopic;

    @Override
    @Async
    public void sendNotification(Order order) {
        try {
            String email = userClient.fetchMyProfile(order.getUserId()).getEmail();
            sendEvent(order, email);
            log.info("Notification sent for order {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send notification for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private void sendEvent(Order order, String email) {
        String message = order.getStatus() == Order.Status.PAID ? "Order paid successfully" : "Order payment failed";
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .email(email)
                .price(order.getPrice())
                .status(order.getStatus().name())
                .message(message)
                .build();
        kafkaTemplate.send(orderTopic, order.getUserId().toString(), event);
    }
}
