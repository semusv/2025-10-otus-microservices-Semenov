package com.microservices.warehouse.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    public static class CompensationResponseEvent {

        private UUID sagaId;

        private UUID orderId;

        private boolean success;

        private String errorMessage;

        private boolean duplicate = false;

        private LocalDateTime timestamp = LocalDateTime.now();
    }
}
