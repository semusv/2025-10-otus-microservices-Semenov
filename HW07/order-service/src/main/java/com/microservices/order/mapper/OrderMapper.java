package com.microservices.order.mapper;

import com.microservices.order.dto.OrderResponse;
import com.microservices.order.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {
    OrderResponse toOrderResponse(Order order);
}
