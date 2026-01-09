package com.microservices.order.service;

import com.microservices.order.dto.CreateOrderRequest;
import com.microservices.order.dto.OrderResponse;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(UUID userId, CreateOrderRequest orderRequest);

    OrderResponse get(UUID orderId, UUID userId);

    List<OrderResponse> getList(UUID userId);
}
