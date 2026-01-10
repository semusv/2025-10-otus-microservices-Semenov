package com.microservices.order.kafka;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderEvent {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private EventType eventType = EventType.ORDER_CREATED;

    @Builder.Default
    private String timestamp = OffsetDateTime.now(ZoneOffset.UTC).format(FORMATTER);

    private UUID orderId;

    private UUID userId;

    private String email;

    private BigDecimal price;

    private String status;

    private String message;
}
