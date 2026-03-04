package com.microservices.order.service;

import java.util.List;
import java.util.UUID;
import ru.vvsem.shared.dto.shared_api_dto.OrderCreateOrderRequest;
import ru.vvsem.shared.dto.shared_api_dto.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(UUID userId, OrderCreateOrderRequest orderRequest);

    OrderResponse get(UUID orderId, UUID userId);

    List<OrderResponse> getList(UUID userId);
}
