package com.microservices.billing.service;

import com.microservices.billing.kafka.UserEventDto;
import com.microservices.billing.model.Account;
import java.math.BigDecimal;
import java.util.UUID;
import ru.vvsem.shared.dto.shared_api_dto.BillingBalanceResponse;

public interface AccountService {
    void createAccountIfMissing(UserEventDto event);

    BillingBalanceResponse deposit(UUID userId, BigDecimal amount);

    BillingBalanceResponse withdraw(UUID userId, BigDecimal amount);

    void withdraw(Account account, BigDecimal amount);

    void deposit(Account account, BigDecimal amount);

    BillingBalanceResponse balance(UUID userId);

    Account getAccountForUser(UUID userId);
}
