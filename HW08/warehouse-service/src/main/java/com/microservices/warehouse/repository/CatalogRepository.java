package com.microservices.warehouse.repository;

import com.microservices.warehouse.model.Catalog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogRepository extends JpaRepository<Catalog, UUID> {

    List<Catalog> findByShopIdOrderByProductNameAsc(UUID shopId);
}
