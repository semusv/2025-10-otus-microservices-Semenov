package com.microservices.order.dto;

import com.microservices.order.model.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private UUID id;

    private UUID userId;

    private BigDecimal price;

    private Order.Status status;

    private LocalDateTime createdAt;
}
