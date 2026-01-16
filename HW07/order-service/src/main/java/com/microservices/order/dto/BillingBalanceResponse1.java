package com.microservices.order.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillingBalanceResponse1 {
    private UUID userId;

    private BigDecimal balance;
}
