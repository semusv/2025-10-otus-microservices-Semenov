package com.microservices.order.kafka;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private UUID orderId;

    private UUID userId;

    private String email;

    private BigDecimal price;

    private String status;

    private String message;
}
