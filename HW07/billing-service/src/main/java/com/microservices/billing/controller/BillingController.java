package com.microservices.billing.controller;

import com.microservices.billing.dto.AmountRequest;
import com.microservices.billing.dto.BalanceResponse;
import com.microservices.billing.service.AccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing/accounts")
@RequiredArgsConstructor
public class BillingController {

    private final AccountService accountService;

    @PostMapping("/deposit")
    public BalanceResponse deposit(@RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody AmountRequest request) {
        return accountService.deposit(userId, request.getAmount());
    }

    @PostMapping("/withdraw")
    public BalanceResponse withdraw(
            @RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody AmountRequest request) {
        return accountService.withdraw(userId, request.getAmount());
    }

    @GetMapping("/balance")
    public BalanceResponse balance(@RequestHeader("X-User-Id") UUID userId) {
        return accountService.balance(userId);
    }
}
