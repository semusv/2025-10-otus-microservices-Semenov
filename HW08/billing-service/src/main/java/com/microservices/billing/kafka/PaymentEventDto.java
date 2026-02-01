package com.microservices.billing.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PaymentEventDto {
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
    public static class CompensationResponseEvent {

        private UUID sagaId;

        private UUID orderId;

        private boolean success;

        private LocalDateTime timestamp = LocalDateTime.now();
    }
}
