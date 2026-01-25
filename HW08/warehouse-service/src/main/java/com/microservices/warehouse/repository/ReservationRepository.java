package com.microservices.warehouse.repository;

import com.microservices.warehouse.model.Reservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {}
