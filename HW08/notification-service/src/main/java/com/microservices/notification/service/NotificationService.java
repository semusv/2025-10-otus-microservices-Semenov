package com.microservices.notification.service;

import com.microservices.notification.kafka.OrderEventDto;
import java.util.List;
import java.util.UUID;
import ru.vvsem.shared.dto.shared_api_dto.NotificationResponse;

public interface NotificationService {
    void saveFromEvent(OrderEventDto event);

    List<NotificationResponse> getNotifications(UUID userId);

    List<NotificationResponse> getNotifications(UUID userId, UUID orderId);
}
