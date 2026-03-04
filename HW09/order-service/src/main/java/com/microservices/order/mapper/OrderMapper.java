package com.microservices.order.mapper;

import com.microservices.order.model.Order;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.vvsem.shared.dto.shared_api_dto.OrderResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(order.getCreatedAt()))")
    @Mapping(target = "status", expression = "java(toStatusEnum(order.getStatus()))")
    OrderResponse toOrderResponse(Order order);

    default OffsetDateTime toOffsetDateTime(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }

    default OrderResponse.StatusEnum toStatusEnum(Order.Status status) {
        if (status == null) {
            return null;
        }
        return OrderResponse.StatusEnum.fromValue(status.name());
    }
}
