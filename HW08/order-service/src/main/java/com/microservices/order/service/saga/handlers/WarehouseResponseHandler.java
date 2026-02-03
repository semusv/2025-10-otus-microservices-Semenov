package com.microservices.order.service.saga.handlers;

import com.microservices.order.kafka.SagaEvents.WarehouseReservationResponseEvent;
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
public class WarehouseResponseHandler {

    private final SagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final SagaStateMachine stateMachine;

    public void processWarehouseResponse(WarehouseReservationResponseEvent event, OrderSaga saga) {
        if (event.isSuccess()) {
            Order order = orderRepository
                    .findById(saga.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
            order.setStatus(Order.Status.RESERVED);
            orderRepository.save(order);
            saga.markWarehouseExecuted();
            saga.setState(OrderSaga.SagaState.WAREHOUSE_RESERVED);
            sagaRepository.save(saga);

            stateMachine.process(saga, order);
            sagaRepository.save(saga);
            log.info("Warehouse reservation successful, saga {} moved to delivery scheduling", saga.getSagaId());
        } else {
            saga.setState(OrderSaga.SagaState.WAREHOUSE_FAILED);
            saga.setErrorMessage("Warehouse reservation failed: " + event.getErrorMessage());
            sagaRepository.save(saga);
        }
    }
}
