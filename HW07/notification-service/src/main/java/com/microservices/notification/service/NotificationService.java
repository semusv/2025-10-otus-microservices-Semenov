package com.microservices.notification.service;

import com.microservices.notification.dto.NotificationResponse;
import com.microservices.notification.kafka.OrderEvent;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void saveFromEvent(OrderEvent event);

    List<NotificationResponse> getNotifications(UUID userId);

    List<NotificationResponse> getNotifications(UUID userId, UUID orderId);
}
