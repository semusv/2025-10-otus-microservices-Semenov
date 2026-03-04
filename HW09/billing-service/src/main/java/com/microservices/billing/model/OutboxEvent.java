package com.microservices.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEvent {

    @Id
    @Column(name = "event_id")
    private String eventId = UUID.randomUUID().toString();

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId; // orderId или sagaId

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType; // "Order", "Saga"

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "event_type", nullable = false)
    private EventType eventType; // "OrderCreated", "PaymentRequested"

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "topic")
    private String topic;

    @Column(name = "published", nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(
            name = "created_at",
            updatable = false,
            nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
