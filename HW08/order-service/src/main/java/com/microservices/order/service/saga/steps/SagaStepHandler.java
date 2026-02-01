package com.microservices.order.service.saga.steps;

import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;

public interface SagaStepHandler {
    void execute(OrderSaga saga, Order order);

    void compensate(OrderSaga saga, Order order, String reason);

    boolean canHandle(OrderSaga.SagaState state);

    OrderSaga.SagaState getHandledState();
}
