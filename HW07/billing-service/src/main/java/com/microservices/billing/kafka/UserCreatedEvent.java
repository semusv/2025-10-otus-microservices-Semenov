package com.microservices.billing.kafka;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreatedEvent {

    private String eventId;

    private EventType eventType;

    private String timestamp;

    private String source;

    private UUID userId;

    private String email;

    private String username;
}
