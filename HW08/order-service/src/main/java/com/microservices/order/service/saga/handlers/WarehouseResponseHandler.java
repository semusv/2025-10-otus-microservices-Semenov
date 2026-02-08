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
        // нужно проверить, что не было обработано ранее
        if (saga.getState() != OrderSaga.SagaState.WAREHOUSE_RESERVING) {
            log.info(
                    "Warehouse reservation response received for saga {}, but saga is not in WAREHOUSE_RESERVING state",
                    saga.getSagaId());
            return;
        }
        if (event.isSuccess()) {
            processSuccessEvent(saga);
        } else {
            saga.setState(OrderSaga.SagaState.WAREHOUSE_FAILED);
            saga.setErrorMessage("Warehouse reservation failed: " + event.getErrorMessage());
            sagaRepository.save(saga);
        }
    }

    private void processSuccessEvent(OrderSaga saga) {
        Order order = orderRepository
                .findById(saga.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
        order.setStatus(Order.Status.RESERVED);
        orderRepository.save(order);
        saga.markWarehouseExecuted(true);
        saga.setState(OrderSaga.SagaState.WAREHOUSE_RESERVED);
        sagaRepository.save(saga);

        stateMachine.process(saga, order);
        sagaRepository.save(saga);
        log.info("Warehouse reservation successful, saga {} moved to delivery scheduling", saga.getSagaId());
    }
}
