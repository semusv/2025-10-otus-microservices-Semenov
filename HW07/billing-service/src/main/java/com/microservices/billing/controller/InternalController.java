package com.microservices.billing.controller;

import com.microservices.billing.service.AccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.vvsem.shared.dto.shared_api_dto.BillingBalanceResponse;
import ru.vvsem.shared.dto.shared_api_dto.BillingOrderRequest;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final AccountService accountService;

    @PostMapping("/order")
    public BillingBalanceResponse withdraw(
            @RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody BillingOrderRequest orderRequest) {
        return accountService.orderPayment(userId, orderRequest);
    }
}
