package com.microservices.warehouse.repository;

import com.microservices.warehouse.model.Product;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Override
    List<Product> findAll();
}
