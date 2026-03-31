package com.microservices.billing.repository;

import com.microservices.billing.model.Operation;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OperationRepository extends JpaRepository<Operation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Operation> findBySagaIdAndSagaStep(UUID sagaId, Operation.SagaStep sagaStep);
}
