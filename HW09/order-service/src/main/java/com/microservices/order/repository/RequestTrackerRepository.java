package com.microservices.order.repository;

import com.microservices.order.model.RequestTracker;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RequestTrackerRepository extends JpaRepository<RequestTracker, UUID> {

    @Query("SELECT r FROM RequestTracker r WHERE r.idempotencyKey = ?1")
    Optional<RequestTracker> findByIdempotencyKey(UUID idempotencyKey);
}
