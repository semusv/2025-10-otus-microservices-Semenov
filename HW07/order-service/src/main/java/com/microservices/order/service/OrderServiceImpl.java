package com.microservices.order.service;

import com.microservices.order.client.BillingServiceClient;
import com.microservices.order.mapper.OrderMapper;
import com.microservices.order.model.Order;
import com.microservices.order.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vvsem.shared.dto.shared_api_dto.BillingOrderRequest;
import ru.vvsem.shared.dto.shared_api_dto.OrderCreateOrderRequest;
import ru.vvsem.shared.dto.shared_api_dto.OrderResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final BillingServiceClient billingClient;

    private final EventPublisher eventPublisher;

    @Transactional
    @Override
    public OrderResponse createOrder(UUID userId, OrderCreateOrderRequest orderRequest) {
        Order order = new Order();
        order.setUserId(userId);
        order.setPrice(orderRequest.getPrice());
        order.setStatus(Order.Status.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order);

        boolean paid = withdraw(new BillingOrderRequest()
                .userId(order.getUserId())
                .orderId(order.getId())
                .price(order.getPrice()));
        order.setStatus(paid ? Order.Status.PAID : Order.Status.FAILED);
        orderRepository.save(order);

        eventPublisher.sendNotification(order);

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderResponse get(UUID orderId, UUID userId) {
        Order order =
                orderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have access to this order");
        }
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getList(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    private boolean withdraw(BillingOrderRequest request) {
        try {
            billingClient.withdrawMoney(request);
            return true;
        } catch (Exception e) {
            log.warn("Billing withdraw failed: {}", e.getMessage());
            return false;
        }
    }
}
