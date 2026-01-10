package com.microservices.auth.kafka;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCreatedEvent {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private EventType eventType = EventType.USER_CREATED;

    @Builder.Default
    private String timestamp = OffsetDateTime.now(ZoneOffset.UTC).format(FORMATTER);

    @Builder.Default
    private String source = "auth-service";

    private UUID userId;

    private String email;

    private String username;
}
