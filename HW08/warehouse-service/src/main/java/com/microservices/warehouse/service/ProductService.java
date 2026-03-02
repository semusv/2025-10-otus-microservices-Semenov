package com.microservices.warehouse.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import ru.vvsem.shared.dto.shared_api_dto.WarehouseProductResponse;

public interface ProductService {
    WarehouseProductResponse getProductById(UUID productId);

    Page<WarehouseProductResponse> getAllProducts(int page, int size);
}
