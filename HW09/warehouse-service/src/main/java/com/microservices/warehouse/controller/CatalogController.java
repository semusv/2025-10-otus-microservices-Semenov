package com.microservices.warehouse.controller;

import com.microservices.warehouse.dto.BatchQuantityUpdateRequest;
import com.microservices.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.vvsem.shared.dto.shared_api_dto.WarehouseCatalogResponse;

@RestController
@RequestMapping("/warehouse/catalogs")
@RequiredArgsConstructor
public class CatalogController {

    private final WarehouseService warehouseService;

    @Operation(summary = "Получить каталог магазина по ID")
    @GetMapping("/shop/{shopId}")
    @ResponseStatus(HttpStatus.OK)
    public List<WarehouseCatalogResponse> getCatalogByShopId(
            @Parameter(description = "ID магазина", required = true) @PathVariable UUID shopId) {

        return warehouseService.getCatalogByShopId(shopId);
    }

    @Operation(summary = "Массовое добавление количества к нескольким элементам каталога")
    @PostMapping("/batch/quantity/add")
    @ResponseStatus(HttpStatus.OK)
    public List<WarehouseCatalogResponse> batchAddQuantity(
            @Parameter(description = "Список запросов на добавление количества", required = true) @RequestBody @Valid
                    BatchQuantityUpdateRequest request) {
        return warehouseService.batchAddQuantity(request);
    }
}
