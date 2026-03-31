package com.microservices.order.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class SagaEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderCreatedEvent {
        private UUID eventId;

        private UUID orderId;

        private UUID userId;

        private BigDecimal totalAmount;

        private LocalDateTime createdAt;

        private List<OrderItem> items;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OrderItem {
            private UUID catalogItemId;

            private Integer quantity;

            private BigDecimal price;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentRequestEvent {
        private UUID sagaId;

        private UUID orderId;

        private UUID userId;

        private BigDecimal amount;

        private String description;

        private LocalDateTime timestamp = LocalDateTime.now();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentResponseEvent {

        private UUID sagaId;

        private UUID orderId;

        private boolean success;

        private String transactionId;

        private String errorMessage;

        private boolean duplicate = false;

        private LocalDateTime timestamp = LocalDateTime.now();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompensationResponseEvent {

        private UUID sagaId;

        private UUID orderId;

        private boolean success;

        private String errorMessage;

        private boolean duplicate = false;

        private LocalDateTime timestamp = LocalDateTime.now();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WarehouseReservationRequestEvent {
        private UUID sagaId;

        private UUID orderId;

        private UUID userId;

        private List<ReservationItem> items;

        private LocalDateTime timestamp = LocalDateTime.now();

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ReservationItem {
            private UUID catalogItemId;

            private Integer quantity;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WarehouseReservationResponseEvent {

        private UUID sagaId;

        private UUID orderId;

        private UUID reservationId;

        private boolean success;

        private String errorMessage;

        private List<ReservationItemStatus> items;

        private LocalDateTime timestamp = LocalDateTime.now();

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ReservationItemStatus {
            private UUID catalogItemId;

            private boolean reserved;

            private Integer reservedQuantity;

            private String error;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryRequestEvent {
        private UUID sagaId;

        private UUID orderId;

        private UUID userId;

        private String deliveryAddress;

        private LocalDateTime deliveryDate;

        private List<DeliveryItem> items;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DeliveryItem {
            private UUID productId;

            private String productName;

            private Integer quantity;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryResponseEvent {
        private UUID sagaId;

        private UUID orderId;

        private UUID deliveryId;

        private boolean success;

        private String errorMessage;

        private LocalDateTime estimatedDelivery;

        private LocalDateTime timestamp = LocalDateTime.now();
    }

    // Компенсирующие события
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentRefundEvent {

        private UUID sagaId;

        private UUID orderId;

        private UUID transactionId;

        private BigDecimal amount;

        private String reason;

        private LocalDateTime timestamp = LocalDateTime.now();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WarehouseReleaseEvent {
        private UUID sagaId;

        private UUID orderId;

        private String reason;

        private LocalDateTime timestamp = LocalDateTime.now();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderFailedEvent {
        private UUID sagaId;

        private UUID orderId;

        private UUID userId;

        private String reason;

        private LocalDateTime timestamp = LocalDateTime.now();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryReservationRequestEvent {
        private UUID sagaId;

        private UUID orderId;

        private UUID userId;

        @Builder.Default
        private LocalDateTime timestamp = LocalDateTime.now();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryReservationResponseEvent {

        private UUID sagaId;

        private UUID orderId;

        private UUID reservationId;

        private boolean success;

        private String errorMessage;

        @Builder.Default
        private LocalDateTime timestamp = LocalDateTime.now();
    }
}
