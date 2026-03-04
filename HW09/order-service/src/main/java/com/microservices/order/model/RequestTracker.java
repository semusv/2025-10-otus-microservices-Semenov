package com.microservices.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "request_trackers")
@Getter
@Setter
public class RequestTracker {

    public enum Status {
        PENDING, // Запрос принят и обрабатывается
        PROCESSED, // Запрос успешно обработан (идемпотентный)
        FAILED // Запрос не удался
    }

    @Id
    @Column(name = "idempotency_key", unique = true, nullable = false)
    private UUID idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(
            name = "created_at",
            updatable = false,
            nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    public void markAsProcessed() {
        this.status = Status.PROCESSED;
    }

    public boolean isDuplicateRequest() {
        return this.status == Status.PROCESSED || this.status == Status.PENDING;
    }
}
