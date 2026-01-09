package com.microservices.notification.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;

    private UUID userId;

    private String email;

    private String subject;

    private String message;

    private String status;
}
