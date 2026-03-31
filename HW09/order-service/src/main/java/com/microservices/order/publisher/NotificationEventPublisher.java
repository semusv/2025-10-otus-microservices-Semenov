package com.microservices.order.publisher;

import com.microservices.order.model.Order;
import ru.vvsem.shared.dto.shared_api_dto.UserProfileResponse;

public interface NotificationEventPublisher {
    void sendNotification(Order order, UserProfileResponse userProfile, String requestId);
}
