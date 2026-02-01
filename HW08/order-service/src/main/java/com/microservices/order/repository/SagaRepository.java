package com.microservices.order.repository;

import com.microservices.order.model.OrderSaga;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SagaRepository extends JpaRepository<OrderSaga, UUID> {
    List<OrderSaga> findByStateIn(Collection<OrderSaga.SagaState> states);

    @Query("SELECT s FROM OrderSaga s " + "WHERE s.state IN :states ")
    List<OrderSaga> findStuckSagas(@Param("states") List<OrderSaga.SagaState> states);
}
