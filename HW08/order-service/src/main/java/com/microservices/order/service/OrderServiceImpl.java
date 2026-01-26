package com.microservices.order.service;

import com.microservices.order.client.BillingServiceClient;
import com.microservices.order.mapper.OrderMapper;
import com.microservices.order.model.Order;
import com.microservices.order.model.Position;
import com.microservices.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vvsem.shared.dto.shared_api_dto.BillingOrderRequest;
import ru.vvsem.shared.dto.shared_api_dto.OrderCreateOrderRequest;
import ru.vvsem.shared.dto.shared_api_dto.OrderCreateOrderRequestItemsInner;
import ru.vvsem.shared.dto.shared_api_dto.OrderResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Value("${spring.application.name}")
    private String serviceName;

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final BillingServiceClient billingClient;

    private final ApplicationEventPublisher eventPublisher;

    private final OrderSagaOrchestrator orderSagaOrchestrator;

    @Transactional
    @Override
    public OrderResponse createOrder(UUID userId, OrderCreateOrderRequest orderRequest) {
        // 1. Создаем заказ в статусе PENDING
        Order order = createNewOrder(userId, orderRequest);
        orderSagaOrchestrator.startOrderSaga(order);
        // 2. Обновляем статус заказа в зависимости от результата списания средств
        //        boolean paid = withdraw(order);
        //        order.setStatus(paid ? Order.Status.PAID : Order.Status.FAILED);
        //        updateOrderStatus(order.getId(), order.getStatus());

        //        eventPublisher.publishEvent(new OrderCreatedEvent(order, serviceName, MDC.getCopyOfContextMap()));

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

    private boolean withdraw(Order order) {
        var request = new BillingOrderRequest()
                .userId(order.getUserId())
                .orderId(order.getId())
                .price(order.getPrice());
        try {
            billingClient.withdrawMoney(request);
            return true;
        } catch (Exception e) {
            log.warn("Billing withdraw failed: {}", e.getMessage());
            return false;
        }
    }

    private void updateOrderStatus(UUID orderId, Order.Status status) {
        orderRepository.updateStatusById(status, orderId);
    }

    private Order createNewOrder(UUID userId, OrderCreateOrderRequest orderRequest) {
        Order order = new Order();

        order.setUserId(userId);
        order.setStatus(Order.Status.PENDING);
        order.setPositions(new ArrayList<>());
        orderRequest.getItems().forEach(item -> {
            Position position = new Position();
            position.setOrder(order);
            position.setCatalogItemId(item.getCatalogItemId());
            position.setQuantity(item.getQuantity());
            position.setPrice(item.getPrice());
            order.getPositions().add(position);
            calculateOrderPrice(item, order);
        });

        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order);
        return order;
    }

    private static void calculateOrderPrice(OrderCreateOrderRequestItemsInner item, Order order) {
        if (order.getPrice() == null) {
            order.setPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        } else {
            order.setPrice(order.getPrice().add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
        }
    }
}
