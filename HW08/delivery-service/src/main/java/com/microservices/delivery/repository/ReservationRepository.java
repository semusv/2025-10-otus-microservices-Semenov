package com.microservices.delivery.repository;

import com.microservices.delivery.model.Reservation;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reservation> findBySagaIdAndSagaStep(UUID sagaId, Reservation.SagaStep sagaStep);
}
