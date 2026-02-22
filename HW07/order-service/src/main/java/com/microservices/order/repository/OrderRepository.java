package com.microservices.order.repository;

import com.microservices.order.model.Order;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Transactional
    @Modifying
    @Query("update Order o set o.status = ?1 where o.id = ?2")
    int updateStatusById(Order.Status status, UUID id);

    List<Order> findByUserIdOrderByCreatedAtAsc(UUID userId);
}
