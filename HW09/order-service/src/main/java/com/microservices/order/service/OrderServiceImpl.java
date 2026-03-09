package com.microservices.order.service;

import com.microservices.order.client.BillingServiceClient;
import com.microservices.order.mapper.OrderMapper;
import com.microservices.order.model.Order;
import com.microservices.order.model.Position;
import com.microservices.order.model.RequestTracker;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.repository.RequestTrackerRepository;
import com.microservices.order.service.saga.OrderSagaOrchestrator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final RequestTrackerRepository requestTrackerRepository;

    private final OrderMapper orderMapper;

    private final BillingServiceClient billingClient;

    private final ApplicationEventPublisher eventPublisher;

    private final OrderSagaOrchestrator orderSagaOrchestrator;

    @Transactional
    @Override
    public OrderResponse createOrder(UUID userId, UUID idempotencyKey, OrderCreateOrderRequest orderRequest) {
        // Проверяем, был ли уже этот запрос обработан (идемпотентность)
        Optional<RequestTracker> existingTracker = requestTrackerRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTracker.isPresent()) {
            return getOrderResponseForExistTracker(idempotencyKey, existingTracker);
        }

        // 1. Создаем заказ в статусе PENDING
        Order order = createNewOrder(userId, orderRequest);

        // Сохраняем информацию о запросе и связываем с заказом
        RequestTracker tracker = new RequestTracker();
        tracker.setIdempotencyKey(idempotencyKey);
        tracker.setUserId(userId);
        tracker.setOrderId(order.getId());
        tracker.setStatus(RequestTracker.Status.PENDING);
        requestTrackerRepository.save(tracker);

        // Запускаем saga
        orderSagaOrchestrator.startOrderSaga(order);

        return orderMapper.toOrderResponse(order);
    }

    private OrderResponse getOrderResponseForExistTracker(
            UUID idempotencyKey, Optional<RequestTracker> existingTracker) {
        RequestTracker tracker = existingTracker.get();
        log.info("Idempotent request detected for key: {}", idempotencyKey);
        log.info("Request already being processed for key: {}", idempotencyKey);
        Order order = orderRepository
                .findById(tracker.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
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

    private Order createNewOrder(UUID userId, OrderCreateOrderRequest orderRequest) {
        Order order = new Order();

        order.setUserId(userId);
        order.setStatus(Order.Status.CREATED);
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
