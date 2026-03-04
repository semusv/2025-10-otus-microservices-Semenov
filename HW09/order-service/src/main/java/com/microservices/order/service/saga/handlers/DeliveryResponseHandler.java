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
        // нужно проверить, что не было обработано ранее
        if (saga.getState() != OrderSaga.SagaState.DELIVERY_RESERVING) {
            log.info(
                    "Delivery response received for saga {}, but saga is not in DELIVERY_RESERVED state",
                    saga.getSagaId());
            return;
        }
        if (event.isSuccess()) {
            processSuccessEvent(saga);
        } else {
            saga.setState(OrderSaga.SagaState.DELIVERY_FAILED);
            saga.setErrorMessage("Delivery failed: " + event.getErrorMessage());
            sagaRepository.save(saga);
        }
    }

    private void processSuccessEvent(OrderSaga saga) {
        Order order = orderRepository
                .findById(saga.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
        order.setStatus(Order.Status.PROCESSING);
        orderRepository.save(order);
        saga.markDeliveryExecuted(true);
        saga.setState(OrderSaga.SagaState.DELIVERY_RESERVED);
        sagaRepository.save(saga);

        stateMachine.process(saga, order);
        sagaRepository.save(saga);
        log.info("Delivery successful, saga {} moved to completed", saga.getSagaId());
    }
}
