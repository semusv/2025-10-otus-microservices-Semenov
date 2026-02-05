package com.microservices.warehouse.repository;

import com.microservices.warehouse.model.Catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;


public interface CatalogRepository extends JpaRepository<Catalog, UUID> {

    List<Catalog> findByShopIdOrderByProductNameAsc(UUID shopId);


    @Query("select c from Catalog c where c.id = ?1")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Catalog> findByIdWithLock(UUID uuid);
}
