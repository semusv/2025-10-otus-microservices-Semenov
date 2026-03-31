package com.microservices.warehouse.mapper;

import com.microservices.warehouse.model.Product;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.vvsem.shared.dto.shared_api_dto.WarehouseProductResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {

    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(product.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toOffsetDateTime(product.getCreatedAt()))")
    WarehouseProductResponse toWarehouseProductResponse(Product product);

    default OffsetDateTime toOffsetDateTime(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }
}
