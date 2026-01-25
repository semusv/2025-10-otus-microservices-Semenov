package com.microservices.warehouse.repository;

import com.microservices.warehouse.model.ReservationItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationItemRepository extends JpaRepository<ReservationItem, UUID> {}
