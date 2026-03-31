package com.microservices.warehouse.controller;

import com.microservices.warehouse.dto.PageResponseDto;
import com.microservices.warehouse.service.ProductService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.vvsem.shared.dto.shared_api_dto.WarehouseProductResponse;

@RestController
@RequestMapping("/warehouse/products")
@RequiredArgsConstructor
public class ProductsController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<WarehouseProductResponse> getProductById(@PathVariable UUID productId) {

        WarehouseProductResponse product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponseDto<WarehouseProductResponse> getAllProducts(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        Page<WarehouseProductResponse> result = productService.getAllProducts(page, size);
        return PageResponseDto.from(result);
    }
}
