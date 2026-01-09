package com.microservices.order.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BillingOrderRequest {

    private UUID userId;

    private UUID orderId;

    @DecimalMin(value = "0.01", message = "Price must be positive")
    private BigDecimal price;
}
