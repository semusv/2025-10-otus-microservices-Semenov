package com.microservices.order.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCreateOrderRequest1 {
    @DecimalMin(value = "0.01", message = "Price must be positive")
    private BigDecimal price;
}
