package com.microservices.delivery.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
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

        @Builder.Default
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

        @Builder.Default
        private boolean duplicate = false;

        @Builder.Default
        private LocalDateTime timestamp = LocalDateTime.now();
    }
}
