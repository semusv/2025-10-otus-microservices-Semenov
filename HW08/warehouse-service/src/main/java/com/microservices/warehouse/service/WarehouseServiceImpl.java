package com.microservices.warehouse.service;

import com.microservices.warehouse.dto.BatchQuantityUpdateRequest;
import com.microservices.warehouse.mapper.CatalogMapper;
import com.microservices.warehouse.model.Catalog;
import com.microservices.warehouse.repository.CatalogRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vvsem.shared.dto.shared_api_dto.WarehouseCatalogResponse;

@RequiredArgsConstructor
@Service
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private final CatalogRepository catalogRepository;

    private final CatalogMapper catalogMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseCatalogResponse> getCatalogByShopId(UUID shopId) {

        return catalogRepository.findByShopIdOrderByProductNameAsc(shopId).stream()
                .map(catalogMapper::toWarehouseCatalogResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<WarehouseCatalogResponse> batchAddQuantity(BatchQuantityUpdateRequest request) {
        List<UUID> catalogIds = prepareCatalogsIds(request);
        Map<UUID, Integer> quantityToAddMap = prepareUpdatedQuantitiesMap(request);
        // Получаем все каталоги по списку ID
        List<Catalog> catalogs = getCatalogItems(catalogIds);
        List<Catalog> updatedCatalogs = new ArrayList<>();

        for (Catalog catalog : catalogs) {
            Integer quantityToAdd = quantityToAddMap.get(catalog.getId());
            if (quantityToAdd != null) {
                int newQuantity = catalog.getQuantity() + quantityToAdd;
                catalog.setQuantity(newQuantity);
                updatedCatalogs.add(catalog);
                log.info("Added {} items to catalog {}. New quantity: {}", quantityToAdd, catalog.getId(), newQuantity);
            }
        }
        // Сохраняем все изменения одним запросом saveAll()
        List<Catalog> savedCatalogs = catalogRepository.saveAll(updatedCatalogs);
        return savedCatalogs.stream()
                .map(catalogMapper::toWarehouseCatalogResponse)
                .toList();
    }

    private static Map<UUID, Integer> prepareUpdatedQuantitiesMap(BatchQuantityUpdateRequest request) {
        return request.getUpdates().stream()
                .collect(Collectors.toMap(
                        BatchQuantityUpdateRequest.BatchUpdateItem::getCatalogId,
                        BatchQuantityUpdateRequest.BatchUpdateItem::getQuantity));
    }

    private static List<UUID> prepareCatalogsIds(BatchQuantityUpdateRequest request) {
        return request.getUpdates().stream()
                .map(BatchQuantityUpdateRequest.BatchUpdateItem::getCatalogId)
                .toList();
    }

    private List<Catalog> getCatalogItems(List<UUID> catalogIds) {
        List<Catalog> catalogs = catalogRepository.findByIdIn(catalogIds);
        if (catalogs.size() != catalogIds.size()) {
            // Проверка на несуществующие записи...
            throw new EntityNotFoundException("Some catalogs not found");
        }
        return catalogs;
    }
}
