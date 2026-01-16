package com.microservices.billing.service;

import com.microservices.billing.exception.AccountNotFoundException;
import com.microservices.billing.exception.InsufficientFundsException;
import com.microservices.billing.kafka.UserCreatedEvent;
import com.microservices.billing.model.Account;
import com.microservices.billing.repository.AccountRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vvsem.shared.dto.shared_api_dto.BillingBalanceResponse;
import ru.vvsem.shared.dto.shared_api_dto.BillingOrderRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public void createAccountIfMissing(UserCreatedEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("Received empty user created event, skipping");
            return;
        }
        accountRepository
                .findByUserId(event.getUserId())
                .ifPresentOrElse(acc -> log.info("Account already exists for user {}", event.getUserId()), () -> {
                    Account account = new Account();
                    account.setUserId(event.getUserId());
                    accountRepository.save(account);
                    log.info("Account created for user {} from event", event.getUserId());
                });
    }

    @Transactional
    public BillingBalanceResponse deposit(UUID userId, BigDecimal amount) {
        Account account = getAccount(userId);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        return new BillingBalanceResponse().userId(userId).balance(account.getBalance());
    }

    @Transactional
    public BillingBalanceResponse withdraw(UUID userId, BigDecimal amount) {
        Account account = getAccount(userId);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        return new BillingBalanceResponse().userId(userId).balance(account.getBalance());
    }

    @Transactional(readOnly = true)
    public BillingBalanceResponse balance(UUID userId) {
        Account account = getAccount(userId);
        return new BillingBalanceResponse().userId(userId).balance(account.getBalance());
    }

    private Account getAccount(UUID userId) {
        return accountRepository
                .findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user " + userId));
    }

    public BillingBalanceResponse orderPayment(UUID userId, @Valid BillingOrderRequest orderRequest) {
        BigDecimal amount = orderRequest.getPrice();
        Account account = getAccount(userId);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        return new BillingBalanceResponse().userId(userId).balance(account.getBalance());
    }
}
