package com.microservices.billing.service;

import com.microservices.billing.exception.AccountNotFoundException;
import com.microservices.billing.exception.InsufficientFundsException;
import com.microservices.billing.kafka.UserEventDto;
import com.microservices.billing.model.Account;
import com.microservices.billing.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vvsem.shared.dto.shared_api_dto.BillingBalanceResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    @Override
    public void createAccountIfMissing(UserEventDto event) {
        if (event == null || event.getUserId() == null) {
            log.warn("Received empty user created event, skipping");
            return;
        }
        accountRepository
                .findByUserIdAndLock(event.getUserId())
                .ifPresentOrElse(acc -> log.info("Account already exists for user {}", event.getUserId()), () -> {
                    Account account = new Account();
                    account.setUserId(event.getUserId());
                    accountRepository.save(account);
                    log.info("Account created for user {} from event", event.getUserId());
                });
    }

    @Transactional
    @Override
    public BillingBalanceResponse deposit(UUID userId, BigDecimal amount) {
        Account account = getAccountAndLock(userId);
        return depositToAccount(amount, account);
    }

    @Override
    @Transactional
    public void deposit(Account account, BigDecimal amount) {
        depositToAccount(amount, account);
    }

    @Transactional
    @Override
    public BillingBalanceResponse withdraw(UUID userId, BigDecimal amount) {
        Account account = getAccountAndLock(userId);
        return withdrawFromAccount(amount, account);
    }

    @Override
    @Transactional
    public void withdraw(Account account, BigDecimal amount) {
        withdrawFromAccount(amount, account);
    }

    @Transactional(readOnly = true)
    @Override
    public BillingBalanceResponse balance(UUID userId) {
        Account account = getAccount(userId);
        return new BillingBalanceResponse().userId(userId).balance(account.getBalance());
    }

    private Account getAccountAndLock(UUID userId) {
        return accountRepository
                .findByUserIdAndLock(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user " + userId));
    }

    private Account getAccount(UUID userId) {
        return accountRepository
                .findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user " + userId));
    }

    private BillingBalanceResponse depositToAccount(BigDecimal amount, Account account) {
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        return new BillingBalanceResponse().userId(account.getUserId()).balance(account.getBalance());
    }

    private BillingBalanceResponse withdrawFromAccount(BigDecimal amount, Account account) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        return new BillingBalanceResponse().userId(account.getUserId()).balance(account.getBalance());
    }
}
