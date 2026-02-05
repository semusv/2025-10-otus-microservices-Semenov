package com.microservices.warehouse.repository;

import com.microservices.warehouse.model.ReservationItem;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationItemRepository extends JpaRepository<ReservationItem, UUID> {
    Optional<ReservationItem> findByCatalogId(UUID id);
}
