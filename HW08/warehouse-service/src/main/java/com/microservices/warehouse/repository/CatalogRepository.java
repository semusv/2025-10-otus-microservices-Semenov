package com.microservices.warehouse.repository;

import com.microservices.warehouse.model.Catalog;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogRepository extends JpaRepository<Catalog, UUID> {

    List<Catalog> findByShopIdOrderByProductNameAsc(UUID shopId);

    @Query("select c from Catalog c where c.id = ?1")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Catalog> findByIdWithLock(UUID uuid);

    @Modifying
    @Query("UPDATE Catalog c SET c.quantity = c.quantity + :increment WHERE c.id = :id")
    void incrementQuantityById(@Param("id") UUID id, @Param("increment") Integer increment);
}
