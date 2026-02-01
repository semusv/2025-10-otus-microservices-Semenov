package com.microservices.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "order_sagas")
@Getter
@Setter
public class OrderSaga {

    public enum SagaState {
        STARTED,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        WAREHOUSE_RESERVING,
        WAREHOUSE_RESERVED,
        WAREHOUSE_FAILED,
        DELIVERY_SCHEDULING,
        DELIVERY_SCHEDULED,
        DELIVERY_FAILED,
        COMPLETED,
        COMPENSATING,
        COMPENSATED
    }

    @Id
    @Column(name = "saga_id")
    private UUID sagaId = UUID.randomUUID();

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private SagaState state = SagaState.STARTED;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // JSON с данными

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    // Флаги выполненных шагов
    private boolean paymentExecuted = false;

    private boolean warehouseExecuted = false;

    private boolean deliveryExecuted = false;

    // Таймстампы для отладки
    private LocalDateTime paymentChangedAt;

    private LocalDateTime warehouseChangedAt;

    private LocalDateTime deliveryChangedAt;

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

    @Version
    @Column(name = "version")
    private Integer version;

    // Методы для управления флагами
    public void markPaymentExecuted() {
        this.paymentExecuted = !this.paymentExecuted;
        this.paymentChangedAt = LocalDateTime.now();
    }

    public void markWarehouseExecuted() {
        this.warehouseExecuted = !this.warehouseExecuted;
        this.warehouseChangedAt = LocalDateTime.now();
    }

    public void markDeliveryExecuted() {
        this.deliveryExecuted = !this.deliveryExecuted;
        this.deliveryChangedAt = LocalDateTime.now();
    }

    public void resetAllFlags() {
        this.paymentExecuted = false;
        this.warehouseExecuted = false;
        this.deliveryExecuted = false;
    }

    public boolean isFullyCompensated() {
        return !(this.paymentExecuted || this.warehouseExecuted || this.deliveryExecuted);
    }
}
