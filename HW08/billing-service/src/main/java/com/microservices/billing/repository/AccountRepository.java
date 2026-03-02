package com.microservices.billing.repository;

import com.microservices.billing.model.Account;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    @Query("select a from Account a where a.userId = ?1")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findByUserIdAndLock(UUID userId);

    Optional<Account> findByUserId(UUID userId);
}
