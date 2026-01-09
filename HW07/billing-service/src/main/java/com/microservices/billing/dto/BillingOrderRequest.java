package com.microservices.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillingOrderRequest {

    private UUID userId;

    private UUID orderId;

    @DecimalMin(value = "0.01", message = "Price must be positive")
    private BigDecimal price;
}
