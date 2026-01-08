package com.microservices.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AmountRequest {
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;
}
