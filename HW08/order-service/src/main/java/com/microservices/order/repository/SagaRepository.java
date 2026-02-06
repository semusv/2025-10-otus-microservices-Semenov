package com.microservices.order.repository;

import com.microservices.order.model.OrderSaga;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SagaRepository extends JpaRepository<OrderSaga, UUID> {
    @Query("select o from OrderSaga o where o.sagaId = ?1")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderSaga> findBySagaId(UUID sagaId);

    List<OrderSaga> findByStateIn(Collection<OrderSaga.SagaState> states);

    @Query("SELECT s FROM OrderSaga s " + "WHERE s.state IN :states ")
    List<OrderSaga> findStuckSagas(@Param("states") List<OrderSaga.SagaState> states);
}
