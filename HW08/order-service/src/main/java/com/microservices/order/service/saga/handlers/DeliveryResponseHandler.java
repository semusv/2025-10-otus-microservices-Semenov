package com.microservices.order.service.saga.handlers;

import com.microservices.order.kafka.SagaEvents.DeliveryResponseEvent;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.repository.SagaRepository;
import com.microservices.order.service.saga.SagaStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryResponseHandler {

    private final SagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final SagaStateMachine stateMachine;

    public void processDeliveryResponse(DeliveryResponseEvent event, OrderSaga saga) {
        if (event.isSuccess()) {
            Order order = orderRepository
                    .findById(saga.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
            order.setStatus(Order.Status.DELIVERED);
            orderRepository.save(order);
            saga.markDeliveryExecuted();
            saga.setState(OrderSaga.SagaState.DELIVERY_SCHEDULED);
            sagaRepository.save(saga);

            stateMachine.process(saga, order);
            sagaRepository.save(saga);
            log.info("Delivery successful, saga {} moved to completed", saga.getSagaId());
        } else {
            saga.setState(OrderSaga.SagaState.DELIVERY_FAILED);
            saga.setErrorMessage("Delivery failed: " + event.getErrorMessage());
            sagaRepository.save(saga);
        }
    }
}
