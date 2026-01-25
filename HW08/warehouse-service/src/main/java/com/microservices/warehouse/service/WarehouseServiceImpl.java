package com.microservices.warehouse.service;

import com.microservices.warehouse.mapper.CatalogMapper;
import com.microservices.warehouse.repository.CatalogRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vvsem.shared.dto.shared_api_dto.WarehouseCatalogResponse;

@RequiredArgsConstructor
@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final CatalogRepository catalogRepository;

    private final CatalogMapper catalogMapper;

    @Override
    public List<WarehouseCatalogResponse> getCatalogByShopId(UUID shopId) {

        return catalogRepository.findByShopIdOrderByProductNameAsc(shopId).stream()
                .map(catalogMapper::toWarehouseCatalogRespon)
                .toList();
    }
}
