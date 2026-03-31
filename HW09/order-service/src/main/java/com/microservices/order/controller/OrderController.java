package com.microservices.order.controller;

import com.microservices.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.vvsem.shared.dto.shared_api_dto.OrderCreateOrderRequest;
import ru.vvsem.shared.dto.shared_api_dto.OrderResponse;

@RestController
@RequestMapping("orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody OrderCreateOrderRequest request) {
        UUID key = idempotencyKey != null ? UUID.fromString(idempotencyKey) : null;
        return orderService.createOrder(userId, key, request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID orderId) {
        return orderService.get(orderId, userId);
    }

    @GetMapping
    public List<OrderResponse> getOrders(@RequestHeader("X-User-Id") UUID userId) {
        return orderService.getList(userId);
    }
}
