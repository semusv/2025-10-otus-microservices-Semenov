package com.microservices.warehouse.service;

import com.microservices.warehouse.dto.BatchQuantityUpdateRequest;
import java.util.List;
import java.util.UUID;
import ru.vvsem.shared.dto.shared_api_dto.WarehouseCatalogResponse;

public interface WarehouseService {
    List<WarehouseCatalogResponse> getCatalogByShopId(UUID shopId);

    List<WarehouseCatalogResponse> batchAddQuantity(BatchQuantityUpdateRequest request);
}
