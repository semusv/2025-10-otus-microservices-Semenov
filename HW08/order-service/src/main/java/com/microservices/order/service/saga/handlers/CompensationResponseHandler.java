package com.microservices.order.service.saga.handlers;

import com.microservices.order.kafka.SagaEvents.CompensationResponseEvent;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.repository.SagaRepository;
import com.microservices.order.service.saga.SagaCompensationExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompensationResponseHandler {

    private final SagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final SagaCompensationExecutor compensationExecutor;

    @Transactional
    public void processPaymentCompensationResponse(CompensationResponseEvent event, OrderSaga saga) {
        if (event.isSuccess()) {
            Order order = orderRepository
                    .findById(saga.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
            compensationExecutor.compensatePayment(saga);
            sagaRepository.save(saga);

            markOrder(order, saga.getState());

            log.info("Payment compensation successful, saga {} moved to {}", saga.getSagaId(), saga.getState());
        } else {
            log.warn("Payment compensation failed: {}", saga.getSagaId());
        }
    }

    @Transactional
    public void processWarehouseCompensationResponse(CompensationResponseEvent event, OrderSaga saga) {
        if (event.isSuccess()) {
            Order order = orderRepository
                    .findById(saga.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
            compensationExecutor.compensateWarehouse(saga);
            sagaRepository.save(saga);

            markOrder(order, saga.getState());

            log.info("Warehouse compensation successful, saga {} moved to {}", saga.getSagaId(), saga.getState());
        } else {
            log.warn("Warehouse compensation failed: {}", saga.getSagaId());
        }
    }

    @Transactional
    public void processDeliveryCompensationResponse(CompensationResponseEvent event, OrderSaga saga) {
        if (event.isSuccess()) {
            Order order = orderRepository
                    .findById(saga.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
            compensationExecutor.compensateDelivery(saga);
            sagaRepository.save(saga);
            markOrder(order, saga.getState());

            log.info("Delivery compensation successful, saga {} moved to {}", saga.getSagaId(), saga.getState());
        } else {
            log.warn("Delivery compensation failed: {}", saga.getSagaId());
        }
    }

    private void markOrder(Order order, OrderSaga.SagaState state) {
        if (state == OrderSaga.SagaState.COMPENSATED) {
            order.setStatus(Order.Status.CANCELLED);
        } else if (state == OrderSaga.SagaState.COMPENSATING) {
            order.setStatus(Order.Status.CANCELLING);
        }
        orderRepository.save(order);
    }
}
