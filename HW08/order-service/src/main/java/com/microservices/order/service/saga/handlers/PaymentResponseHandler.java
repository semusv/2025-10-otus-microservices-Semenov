package com.microservices.order.service.saga.handlers;

import com.microservices.order.kafka.SagaEvents.PaymentResponseEvent;
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
public class PaymentResponseHandler {

    private final SagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final SagaStateMachine stateMachine;

    public void processPaymentResponse(PaymentResponseEvent event, OrderSaga saga) {
        if (event.isSuccess()) {
            Order order = orderRepository
                    .findById(saga.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
            order.setStatus(Order.Status.PAID);
            orderRepository.save(order);
            saga.markPaymentExecuted();
            saga.setState(OrderSaga.SagaState.PAYMENT_COMPLETED);
            sagaRepository.save(saga);

            stateMachine.process(saga, order);
            sagaRepository.save(saga);
            log.info("Payment successful, saga {} moved to warehouse reservation", saga.getSagaId());
        } else {
            saga.setState(OrderSaga.SagaState.PAYMENT_FAILED);
            saga.setErrorMessage("Payment failed: " + event.getErrorMessage());
            sagaRepository.save(saga);
        }
    }
}
