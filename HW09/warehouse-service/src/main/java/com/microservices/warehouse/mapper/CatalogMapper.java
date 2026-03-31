package com.microservices.warehouse.mapper;

import com.microservices.warehouse.model.Catalog;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.vvsem.shared.dto.shared_api_dto.WarehouseCatalogResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CatalogMapper {

    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(catalog.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toOffsetDateTime(catalog.getCreatedAt()))")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSku", source = "product.sku")
    WarehouseCatalogResponse toWarehouseCatalogResponse(Catalog catalog);

    default OffsetDateTime toOffsetDateTime(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }
}
