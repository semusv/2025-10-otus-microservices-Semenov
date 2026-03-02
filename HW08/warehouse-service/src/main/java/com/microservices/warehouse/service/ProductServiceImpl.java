package com.microservices.warehouse.service;

import com.microservices.warehouse.mapper.ProductMapper;
import com.microservices.warehouse.model.Product;
import com.microservices.warehouse.repository.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.vvsem.shared.dto.shared_api_dto.WarehouseProductResponse;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private final ProductMapper productMapper;

    @Override
    public WarehouseProductResponse getProductById(UUID productId) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new RuntimeException(String.format("Product with id %s not found", productId)));
        return productMapper.toWarehouseProductResponse(product);
    }

    @Override
    public Page<WarehouseProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "name"));
        Page<Product> products = productRepository.findAll(pageable);

        return products.map(productMapper::toWarehouseProductResponse);
    }
}
