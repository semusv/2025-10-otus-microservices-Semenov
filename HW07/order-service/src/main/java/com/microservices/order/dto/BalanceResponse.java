package com.microservices.order.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BalanceResponse {
    private UUID userId;

    private BigDecimal balance;
}
