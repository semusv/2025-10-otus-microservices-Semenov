package com.microservices.notification.controller;

import com.microservices.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vvsem.shared.dto.shared_api_dto.NotificationResponse;

@RestController
@RequestMapping("notif")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> list(
            @RequestHeader("X-User-Id") UUID userId, @RequestParam(name = "orderId", required = false) UUID orderId) {
        if (orderId != null) {
            return notificationService.getNotifications(userId, orderId);
        }
        return notificationService.getNotifications(userId);
    }
}
