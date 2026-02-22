package com.microservices.notification.repository;

import com.microservices.notification.model.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByOrderIdAndUserIdOrderByCreatedAtDesc(UUID orderId, UUID userId);

    List<Notification> findAllByUserId(UUID userId);
}
