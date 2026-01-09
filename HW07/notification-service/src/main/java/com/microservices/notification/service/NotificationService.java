package com.microservices.notification.service;

import com.microservices.notification.dto.NotificationResponse;
import com.microservices.notification.kafka.OrderEvent;
import com.microservices.notification.mapper.NotificationMapper;
import com.microservices.notification.model.Notification;
import com.microservices.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private final NotificationMapper notificationMapper;

    @Transactional
    public void saveFromEvent(OrderEvent event) {
        Notification notification = new Notification();
        notification.setUserId(event.getUserId());
        notification.setEmail(event.getEmail());
        boolean success = "PAID".equalsIgnoreCase(event.getStatus());
        notification.setSubject(success ? "Order success" : "Order failed");
        notification.setMessage(event.getMessage());
        notification.setStatus(event.getStatus());
        notificationRepository.save(notification);
        log.info("Notification saved for user {}", event.getUserId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(UUID userId) {
        return notificationRepository.findAllByUserId(userId).stream()
                .map(notificationMapper::toNotificationResponse)
                .toList();
    }
}
