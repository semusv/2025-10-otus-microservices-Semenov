package com.microservices.order.repository;

import com.microservices.order.model.Position;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, UUID> {}
