package com.microservices.notification.controller;

import com.microservices.notification.dto.NotificationResponse;
import com.microservices.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
